package utils

/*
 * Copyright (c) 2009-2015. Authors: see NOTICE file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * User: lrollus
 * Date: 17/10/12
 * GIGA-ULg
 * Utility class to deals with file
 */
class FilesUtils {

    /**
     * Get the extension of a filename
     */
    public static def getExtensionFromFilename = {filename ->
        def returned_value = ""
        def m = (filename =~ /(\.[^\.]*)$/)
        if (m.size() > 0) returned_value = ((m[0][0].size() > 0) ? m[0][0].substring(1).trim().toLowerCase() : "");
        return returned_value
    }

    /**
     * Convert the current filename to a valide filename (without bad char like '@','+',...)
     * All bad char are replaced with '_'
     * @param file File
     * @return Correct filename for this file
     */
    public static String correctFileName(def originalFilename) {
        String newFilename = originalFilename
        newFilename = newFilename.replace(" ", "_")
        newFilename = newFilename.replace("(", "_")
        newFilename = newFilename.replace(")", "_")
        newFilename = newFilename.replace("+", "_")
        newFilename = newFilename.replace("*", "_")
        newFilename = newFilename.replace("/", "_")
        newFilename = newFilename.replace("@", "_")
        newFilename = newFilename.replace("'", "_")
        newFilename = newFilename.replace('"', "_")
        newFilename = newFilename.replace('$', "_")
        newFilename = newFilename.replace('€', "_")
        newFilename = newFilename.replace('£', "_")
        newFilename = newFilename.replace('°', "_")
        newFilename = newFilename.replace('`', "_")
        newFilename = newFilename.replace('[', "_")
        newFilename = newFilename.replace(']', "_")
        return newFilename
    }

}
