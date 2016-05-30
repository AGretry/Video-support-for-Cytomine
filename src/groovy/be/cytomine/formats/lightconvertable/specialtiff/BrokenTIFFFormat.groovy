package be.cytomine.formats.lightconvertable.specialtiff

import org.springframework.util.StringUtils

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


/**
 * Created by hoyoux on 16.02.15.
 */
class BrokenTIFFFormat extends TIFFFormat{

    public BrokenTIFFFormat () {
        extensions = ["tif", "tiff"]
    }

    public boolean detect() {
        String tiffinfo = getTiffInfo()

        if (tiffinfo.contains("not a valid IFD offset.")) return true;

        int nbTiffDirectory = StringUtils.countOccurrencesOf(tiffinfo, "TIFF Directory")
        int nbWidth = StringUtils.countOccurrencesOf(tiffinfo, "Image Width:")
        if(nbTiffDirectory  == 2 && nbWidth < 2) return true

        return false
    }
}
