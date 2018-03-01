package be.cytomine.formats.supported

/*
 * Copyright (c) 2009-2017. Authors: see NOTICE file.
 *
 * Licensed under the GNU Lesser General Public License, Version 2.1 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.gnu.org/licenses/lgpl-2.1.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import be.cytomine.formats.Format
import grails.util.Holders
import utils.ServerUtils
import utils.URLBuilder

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

/**
 * Created by stevben on 10/05/14.
 */
abstract class SupportedImageFormat extends Format {

    public String[] extensions = null
    public String mimeType = null
    public String widthProperty = "width"
    public String heightProperty = "height"
    public String resolutionProperty = "resolution"
    public String magnificiationProperty = "magnificiation"
    public List<String> iipURL = ServerUtils.getServers(Holders.config.cytomine.iipImageServerCyto)

    abstract BufferedImage associated(String label)

    abstract BufferedImage thumb(int maxSize)

    String[] associated() {
        return ["macro"]
    }

    def properties() {
        BufferedImage bufferedImage = ImageIO.read(new File(absoluteFilePath))
        def properties = [[key: "mimeType", value: mimeType]]
        properties << [key: "cytomine.width", value: bufferedImage.getWidth()]
        properties << [key: "cytomine.height", value: bufferedImage.getHeight()]
        properties << [key: "cytomine.resolution", value: null]
        properties << [key: "cytomine.magnification", value: null]
        properties << [key: "cytomine.bitdepth", value: 8]
        properties << [key: "cytomine.colorspace", value: null]
        return properties
    }

    String cropURL(def params, def charset = "UTF-8") {
        int topLeftX = params.int('topLeftX')
        int topLeftY = params.int('topLeftY')
        double width = params.double('width')
        double height = params.double('height')
        double imageWidth = params.double('imageWidth')
        double imageHeight = params.double('imageHeight')
        boolean inverse = params.boolean("inverse", false)

        //All values x,y,w & h should be in ratios 0-1.0 [RGN=x,y,w,h]
        def x = (topLeftX == 0) ? 0 : 1 / (imageWidth / topLeftX)
        def y = ((imageHeight - topLeftY) == 0) ? 0 : 1 / (imageHeight / (imageHeight - topLeftY))
        double w = (width == 0) ? 0d : 1d / (imageWidth / width)
        double h = (height == 0) ? 0d : 1d / (imageHeight / height)

        // TODO perf: replace the previous assignment by the following
        /*def x = topLeftX/imageWidth
        def y = (imageHeight - topLeftY)/imageHeight
        double w = width/imageWidth
        double h = height/imageHeight*/

        if (x > 1 || y > 1) return ""

        int maxWidthOrHeight = Holders.config.cytomine.maxCropSize
        if (params.maxSize) {
            int maxSize = params.int('maxSize', 256)
            if (maxWidthOrHeight > maxSize) {
                maxWidthOrHeight = maxSize
            }
        }

        def iipRequest = new URLBuilder(ServerUtils.getServer(iipURL), charset)
        iipRequest.addParameter("FIF", params.fif, true)
        iipRequest.addParameter("RGN", "$x,$y,$w,$h")

        if (ServerUtils.getServers(Holders.config.cytomine.iipImageServerCyto).containsAll(iipURL)) {
            // with new version of iipsrv, the meaning of WID & HEI change !
            if (width > maxWidthOrHeight || height > maxWidthOrHeight) {
                iipRequest.addParameter("HEI", "$maxWidthOrHeight")
                iipRequest.addParameter("WID", "$maxWidthOrHeight")
            }
            else if (params.maxSize) {
                int maxSize = params.int('maxSize', 256)
                iipRequest.addParameter("HEI", "$maxSize")
                iipRequest.addParameter("WID", "$maxSize")
            }
            else {
                iipRequest.addParameter("HEI", "$height")
                iipRequest.addParameter("WID", "$width")
            }
        }
        else {
            if (width > maxWidthOrHeight || height > maxWidthOrHeight) {
                int tmpWidth = width
                int tmpHeight = height
                while (tmpWidth > maxWidthOrHeight || tmpHeight > maxWidthOrHeight) {
                    tmpWidth = tmpWidth / 2
                    tmpHeight = tmpHeight / 2
                }
                /*
                Ruven P. (author of IIP Image)
                In fact, the region is calculated from the WID or HEI given, not from
                the full image size. So you get the requested region on the virtual
                750px resize. I guess you were expecting to get a region exactly of size
                WID?

                This is something that seems to have caused confusion with others also
                and perhaps the way it works in counter intuitive, so I'm considering
                changing the behaviour in the 1.0 release and have WID or HEI define the
                final region size rather than the virtual image size. In the meantime,
                the way to get around it is to calculate the appropriate WID that the
                full image would be. So if your image is x pixels wide, give WID the
                value of x/2 to get a 750px wide image.
                */
                int hei = imageHeight / (height / tmpHeight)
                iipRequest.addParameter("HEI", "$hei")
            }
        }

        if (params.contrast) iipRequest.addParameter("CNT", "$params.contrast")
        if (params.gamma) iipRequest.addParameter("GAM", "$params.gamma")
        if (params.colormap) iipRequest.addParameter("CMP", params.colormap, true)
        if (inverse) iipRequest.addParameter("INV", "true")
        if (params.bits) iipRequest.addParameter("BIT", params.bits)
        iipRequest.addParameter("CVT", params.format)
        return iipRequest.toString()
    }

    String tileURL(fif, params, with_zoomify) {
        if (with_zoomify) {
            return "${ServerUtils.getServer(iipURL)}?zoomify=" + URLEncoder.encode(fif, "UTF-8") +
                    "/TileGroup$params.tileGroup/$params.z-$params.x-$params.y" + ".jpg"
        }
        else {
            def inverse = params.boolean("inverse", false)

            def iipRequest = new URLBuilder(ServerUtils.getServer(iipURL))
            iipRequest.addParameter("FIF", fif, true)
            if (params.contrast) iipRequest.addParameter("CNT", params.contrast)
            if (params.gamma) iipRequest.addParameter("GAM", params.gamma)
            if (params.colormap) iipRequest.addParameter("CMP", params.colormap, true)
            if (inverse) iipRequest.addParameter("INV", "true")

            iipRequest.addParameter("JTL", "$params.z,$params.tileIndex")
            return iipRequest.toString()
        }
    }

    // TODO do it with OpenSlide or IIP ?
    protected BufferedImage rotate90ToRight(BufferedImage inputImage) {
        int width = inputImage.getWidth()
        int height = inputImage.getHeight()
        BufferedImage returnImage = new BufferedImage(height, width, inputImage.getType())

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                returnImage.setRGB(height - y - 1, x, inputImage.getRGB(x, y))
            }
        }
        return returnImage
    }
}
