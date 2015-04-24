package be.cytomine.formats.archive

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

import be.cytomine.formats.ArchiveFormat
import utils.FilesUtils
import utils.ProcUtils

import java.awt.image.BufferedImage

/**
 * Created by stevben on 23/04/14.
 */
class ZipFormat extends ArchiveFormat {

    public boolean detect() {
        String command = "file  $absoluteFilePath"
        def proc = command.execute()
        proc.waitFor()
        String stdout = proc.in.text
        return stdout.contains("Zip archive data")
    }

    public String[] extract(String destPath) {
                /*        long timestamp = new Date().getTime()
        String parentPath = new File(absoluteFilePath).getParent()
        String destPath = ["/tmp", timestamp].join(File.separator)*/

        /* Create and temporary directory which will contains the archive content */
        println "Create path=$destPath"
        ProcUtils.executeOnShell("mkdir -p " + destPath)
        println "Create right=$destPath"
        ProcUtils.executeOnShell("chmod -R 777 " + destPath)

        /* Get extension of filename in order to choose the uncompressor */
        String ext = FilesUtils.getExtensionFromFilename(absoluteFilePath).toLowerCase()
        /* Unzip */
        if (ext == 'zip') {
            def ant = new AntBuilder()
            ant.unzip(src : absoluteFilePath,
                    dest : destPath,
                    overwrite : false)
        }

        def pathsAndExtensions = []
        new File(destPath).eachFileRecurse() { file ->
            if (!file.directory) {
                pathsAndExtensions << file.getAbsolutePath()
            }
        }

        return pathsAndExtensions
    }

}
