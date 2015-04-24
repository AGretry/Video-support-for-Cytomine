package be.cytomine.formats.specialtiff

/*
 * Copyright (c) 2009-2015. Authors: see NOTICE file.
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

import be.cytomine.formats.standard.TIFFFormat
import grails.util.Holders

/**
 * Created by hoyoux on 31.03.15.
 */
class TIFFToConvert extends TIFFFormat {

    public String getTiffInfo() {
        def tiffinfoExecutable = Holders.config.cytomine.tiffinfo
        String tiffinfo = "$tiffinfoExecutable $absoluteFilePath".execute().text
        return tiffinfo;
    }
}
