package be.cytomine.formats.supported.digitalpathology

/*
 * Copyright (c) 2009-2018. Authors: see NOTICE file.
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

/**
 * Created by stevben on 22/04/14.
 */
class AperioSVSFormat extends OpenSlideSingleFileFormat {

    public AperioSVSFormat(){
        extensions = ["svs"]
        vendor = "aperio"
        mimeType = "openslide/svs"
        widthProperty = "openslide.level[0].width"
        heightProperty = "openslide.level[0].height"
        resolutionProperty = "aperio.MPP"
        magnificiationProperty = "aperio.AppMag"
    }
}
