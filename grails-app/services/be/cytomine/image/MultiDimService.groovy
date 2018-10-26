package be.cytomine.image

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

import be.cytomine.multidim.HyperSpectralImage
import ch.systemsx.cisd.hdf5.HDF5Factory
import ch.systemsx.cisd.hdf5.HDF5IntStorageFeatures
import ch.systemsx.cisd.hdf5.IHDF5Reader
import ch.systemsx.cisd.hdf5.IHDF5Writer
import grails.util.Holders

class MultiDimService {

    def convert(def filename, def images, def bpc) {
        HyperSpectralImage hsImage = new HyperSpectralImage(images)

        long maxBlockSize = ((long) Holders.config.cytomine.hdf5.maxBlockSize as Long) * 1000000
        long maxBurstSize = ((long) Holders.config.cytomine.hdf5.maxBurstSize as Long) * 1000000

        int burstLength = Math.min(Math.ceil(maxBurstSize / hsImage.meanChannelsSize()), hsImage.depth)
        int nBursts = Math.ceil(hsImage.depth / burstLength)

        int blockLength = Math.min(512, Math.floor(Math.sqrt(maxBlockSize / (hsImage.depth * bpc / 8))))
        int nBlocksX = Math.ceil(hsImage.width / blockLength)
        int nBlocksY = Math.ceil(hsImage.height / blockLength)
        int nBlocks = nBlocksX * nBlocksY

        def features = new HDF5IntStorageFeatures.HDF5IntStorageFeatureBuilder().chunkedStorageLayout().compress().features()

        log.info "Burst Length: $burstLength"
        log.info "Number of bursts: $nBursts"
        log.info "Block Length: $blockLength"
        log.info "Number of blocks along X axis: $nBlocksX"
        log.info "Number of blocks along Y axis: $nBlocksY"

        // Open output file
        IHDF5Writer hdf5 = HDF5Factory.open("${filename}.h5")
        log.info "Starting to write HDFS file ${filename}.h5"
        int[] meta = [blockLength, bpc, hsImage.depth, hsImage.width, hsImage.height]
        hdf5.int32().writeArray("/meta", meta)

        for (def burst = 0; burst < nBursts; burst++) {
            def startDepth = burst * burstLength
            def endDepth = Math.min(startDepth + burstLength, hsImage.depth)
            log.info "Starting burst ${burst+1}/$nBursts, width depths from $startDepth to $endDepth"

            // Load images for this burst
            def timeLoad = benchmark {
                hsImage.loadChannels((startDepth..(endDepth - 1)))
            }
            log.info "Channels loading: ${endDepth - startDepth} opened in $timeLoad s"

            def timeRead = 0, timeWrite = 0

            for (def block = 0; block < nBlocks; block++) {
                def X = (int) (block / nBlocksY)
                def Y = (int) (block % nBlocksY)
                def x = X * blockLength
                def y = Y * blockLength

                def width = (x + blockLength >= hsImage.width) ? hsImage.width - x : blockLength
                def height = (y + blockLength >= hsImage.height) ? hsImage.height - y : blockLength
                def blockSize = width * height * burstLength

                def data, tRead, tWrite
                tRead = benchmark {
                    data = hsImage.extract(x, y, width, height)
                }

                tWrite = benchmark {
                    // Create array block
                    if (burst == 0) {
                        if (bpc == 32) hdf5.int32().createArray("/b${X}_${Y}", 1, blockSize, features)
                        else if (bpc == 16) hdf5.int16().createArray("/b${X}_${Y}", 1, blockSize, features)
                        else hdf5.int8().createArray("/b${X}_${Y}", 1, blockSize, features)
                    }

                    // Write array block
                    if (bpc == 32) {
                        int[] result = data as Integer[]
                        if (result.length < blockSize) {
                            int[] tmp = new int[blockSize]
                            System.arraycopy(result, 0, tmp, 0, result.length)
                            result = tmp
                        }
                        hdf5.int32().writeArrayBlock("/b${X}_${Y}", result, burst)
                    }
                    else if (bpc == 16) {
                        short[] result = data as Short[]
                        if (result.length < blockSize) {
                            short[] tmp = new short[blockSize]
                            System.arraycopy(result, 0, tmp, 0, result.length)
                            result = tmp
                        }
                        hdf5.int16().writeArrayBlock("/b${X}_${Y}", result, burst)
                    }
                    else {
                        byte[] result = data as Byte[]
                        if (result.length < blockSize) {
                            byte[] tmp = new byte[blockSize]
                            System.arraycopy(result, 0, tmp, 0, result.length)
                            result = tmp
                        }
                        hdf5.int8().writeArrayBlock("/b${X}_${Y}", result, burst)
                    }
                }

                timeRead += tRead
                timeWrite += tWrite

                log.info "Wrote block ${block+1}/$nBlocks (X=${X}, Y=${Y}) - Reading: $tRead s - Writing: $tWrite s"
            }

            log.info "Time for burst ${burst+1}: Reading: $timeRead s - Writing: $timeWrite s"
            log.info "Mean time per block for burst ${burst+1}: Reading: ${timeRead/nBlocks} s - Writing: ${timeWrite/nBlocks}"
            log.info "Total time for burst ${burst+1}: ${timeLoad + timeRead + timeWrite} s"
        }

        hdf5.close()
    }

    def pixelSpectrum(def file, def x, def y, def minChannel, def maxChannel) {
        IHDF5Reader hdf5 = HDF5Factory.openForReading(file)
        int[] meta = hdf5.int32().readArray("/meta")
        def blockLength = meta[0]
        def bpc = meta[1]
        def depth = meta[2]
        def imageWidth = meta[3]
        def imageHeight = meta[4]

        minChannel = Math.max(0, minChannel)
        maxChannel = Math.min(depth, maxChannel)

        x = Math.min(x, imageWidth)
        y = Math.min(y, imageHeight)

        def X = (int) Math.floor(x / blockLength)
        def Y = (int) Math.floor(y / blockLength)

        def xStart = X * blockLength
        def xEnd = Math.min(xStart + blockLength, imageWidth)
        def yStart = Y * blockLength
        def yEnd = Math.min(yStart + blockLength, imageHeight)
        def blockWidth = xEnd - xStart
        def blockHeight = yEnd - yStart

        def reader
        if (bpc == 32) reader = hdf5.int32()
        else if (bpc == 16) reader = hdf5.int16()
        else reader = hdf5.int8()
        def block = reader.readArray("/b${X}_${Y}")

        def res = []
        for (def k = minChannel; k < maxChannel; k++) {
            def i = x % blockWidth
            def j = y % blockHeight
            res << (int) (block[i + blockWidth * j + blockWidth * blockHeight * k] & 0xFF)
        }

        return [[x, y], res]
    }

    def rectangleSpectrum(def file, def x, def y, def width, def height, def minChannel, def maxChannel) {
        IHDF5Reader hdf5 = HDF5Factory.openForReading(file)
        int[] meta = hdf5.int32().readArray("/meta")
        def blockLength = meta[0]
        def bpc = meta[1]
        def depth = meta[2]
        def imageWidth = meta[3]
        def imageHeight = meta[4]

        minChannel = Math.max(depth, minChannel)
        maxChannel = Math.min(depth, maxChannel)

        def reader
        if (bpc == 32) reader = hdf5.int32()
        else if (bpc == 16) reader = hdf5.int16()
        else reader = hdf5.int8()

        def xmin = Math.min(x, imageWidth)
        def ymin = Math.min(y, imageHeight)
        def xmax = Math.min(xmin + width, imageWidth)
        def ymax = Math.min(ymin + height, imageHeight)

        def Xmin = (int) Math.floor(xmin / blockLength)
        def Ymin = (int) Math.floor(ymin / blockLength)
        def Xmax = (int) Math.ceil(xmax / blockLength)
        def Ymax = (int) Math.ceil(ymax / blockLength)

        def spectrum = []
        for (def X = Xmin; X < Xmax; X++) {
            for (def Y = Ymin; Y < Ymax; Y++) {
                def block = reader.readArray("/b${X}_${Y}")

                def xStart = X * blockLength
                def xEnd = Math.min(xStart + blockLength, imageWidth)
                def yStart = Y * blockLength
                def yEnd = Math.min(yStart + blockLength, imageHeight)
                def blockWidth = xEnd - xStart
                def blockHeight = yEnd - yStart

                def x1 = (int) Math.max(xmin, xStart)
                def x2 = (int) Math.min(xmax, xEnd)
                def y1 = (int) Math.max(ymin, yStart)
                def y2 = (int) Math.min(ymax, yEnd)

                for (def i = x1; i < x2; i++) {
                    for (def j  = y1; j < y2; j++) {
                        def res = []
                        for (def k = minChannel; k < maxChannel; k++) {
                            def ii = i % blockWidth
                            def jj = j % blockHeight
                            res << (int) (block[ii + blockWidth * jj + blockWidth * blockHeight * k] & 0xFF)
                        }
                        spectrum << [[i, j], res]
                    }
                }
            }
        }

        return spectrum
    }

    def benchmark = { closure ->
        def start = System.currentTimeMillis()
        closure.call()
        def now = System.currentTimeMillis()
        (now - start) / 1000
    }
}
