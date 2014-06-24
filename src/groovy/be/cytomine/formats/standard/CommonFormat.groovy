package be.cytomine.formats.standard

import be.cytomine.formats.ImageFormat
import grails.util.Holders
import utils.FilesUtils
import utils.ProcUtils

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

/**
 * Created by stevben on 22/04/14.
 */
abstract class CommonFormat extends ImageFormat {

    public IMAGE_MAGICK_FORMAT_IDENTIFIER = null

    public boolean detect() {
        def identifyExecutable = Holders.config.cytomine.identify
        String command = "$identifyExecutable -verbose $absoluteFilePath"
        def proc = command.execute()
        proc.waitFor()
        String stdout = proc.in.text
        return stdout.contains(IMAGE_MAGICK_FORMAT_IDENTIFIER)
    }

    String convert(String workingPath) {
        String ext = FilesUtils.getExtensionFromFilename(absoluteFilePath).toLowerCase()
        String source = absoluteFilePath
        String target = [new File(absoluteFilePath).getParent(), UUID.randomUUID().toString() + ".tif"].join(File.separator)
        String intermediate = [new File(absoluteFilePath).getParent(), "_tmp.tif"].join(File.separator)

        println "ext : $ext"
        println "source : $source"
        println "target : $target"
        println "intermediate : $intermediate"

        //1. Look for vips executable

        def vipsExecutable = Holders.config.cytomine.vips

        //def extractBandCommand = """$vipsExecutable extract_band $source $intermediate[bigtiff,compression=lzw] 0 --n 3"""
        //def rmIntermediatefile = """rm $intermediate"""
        def pyramidCommand = """$vipsExecutable tiffsave "$source" "$target" --tile --pyramid --compression lzw --tile-width 256 --tile-height 256 --bigtiff"""

        boolean success = true

        //success &= (ProcUtils.executeOnShell(extractBandCommand) == 0)

        /*if(!success) {
            success = true
            extractBandCommand = """$vipsExecutable extract_band $source $intermediate[bigtiff,compression=lzw] 0 --n 1"""
            success &= (ProcUtils.executeOnShell(extractBandCommand) == 0)
        }*/

        success &= (ProcUtils.executeOnShell(pyramidCommand) == 0)
        //success &= (ProcUtils.executeOnShell(rmIntermediatefile) == 0)

        if (success) {
            return target
        }
    }

    public BufferedImage associated(String label) { //should be abstract
        if (label == "macro" || label == "preview") {
            thumb(256)
        } else if (label == "preview") {
            thumb(1024)
        }
    }

    public BufferedImage thumb(int maxSize) {
        File thumbnailFile = File.createTempFile("thumbnail", ".jpg")
        def thumbnail_command = """vipsthumbnail $absoluteFilePath --interpolator bicubic --vips-concurrency=8 -o $thumbnailFile.absolutePath"""
        println thumbnail_command
        def proc = thumbnail_command.execute()
        proc.waitFor()
        return ImageIO.read(thumbnailFile)
    }
}
