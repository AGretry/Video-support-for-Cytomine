package be.cytomine.formats.archive

import be.cytomine.formats.ArchiveFormat
import utils.FilesUtils
import utils.ProcUtils

/**
 * Created by stevben on 23/04/14.
 */
class ZipFormat extends ArchiveFormat {

    def DESCRIPTION_EXPECTED = "Zip archive data"

    public boolean detect() {
        String command = "file  $uploadedFilePath"
        def proc = command.execute()
        proc.waitFor()
        String stdout = proc.in.text
        return stdout.contains(DESCRIPTION_EXPECTED)
    }

    public String[] extract() {
        long timestamp = new Date().getTime()
        String parentPath = new File(uploadedFilePath).getParent()
        String destPath = ["/tmp", timestamp].join(File.separator)

        /* Create and temporary directory which will contains the archive content */
        println "Create path=$destPath"
        ProcUtils.executeOnShell("mkdir -p " + destPath)
        println "Create right=$destPath"
        ProcUtils.executeOnShell("chmod -R 777 " + destPath)

        /* Get extension of filename in order to choose the uncompressor */
        String ext = FilesUtils.getExtensionFromFilename(uploadedFilePath).toLowerCase()
        /* Unzip */
        if (ext == 'zip') {
            def ant = new AntBuilder()
            ant.unzip(src : uploadedFilePath,
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
