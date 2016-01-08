package be.cytomine.formats

/*
 * Copyright (c) 2009-2016. Authors: see NOTICE file.
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

class TileService {

    def getTileUrl(def params) {
        String fif = params.zoomify
        /*remove the "/" at the end of the path injected by openlayers (OL2).
          I Did not find the way to avoid it from OL2 (BS)
         */
        if (fif.endsWith("/"))
            fif = fif.substring(0, fif.length()-1)
        String mimeType = params.mimeType
        ImageFormat imageFormat = FormatIdentifier.getImageFormatByMimeType(fif, mimeType)
        return imageFormat.tileURL(fif, params)
    }
}