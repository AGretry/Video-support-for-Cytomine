package be.cytomine.multidim

import be.cytomine.multidim.exceptions.CacheTooSmallException
import be.cytomine.multidim.hdf5.output.FileReaderCache
import grails.converters.JSON
import ncsa.hdf.hdf5lib.exceptions.HDF5FileNotFoundException
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType


@RestApi(name = "Multi dimentional services", description = "Methods for getting the spectra of geormetric forms")
class MultiDimController {


    @RestApiMethod(description="Get the spectra of  a pixel", extensions = [".json"])
    @RestApiParams(params=[
            @RestApiParam(name="fif", type="String", paramType= RestApiParamType.QUERY, description="The absolute path of a multispectral image"),
            @RestApiParam(name="x", type="int", paramType= RestApiParamType.QUERY, description="The X coordinate" ),
            @RestApiParam(name="y", type="int", paramType= RestApiParamType.QUERY, description="The y coordinate" )

    ])
    def getSpectraPixel(){
        def aMap = new HashMap()

        try{
            String name = params.fif
            def coo = [Integer.parseInt(params.x) , Integer.parseInt(params.y)]
            aMap.put("pxl", coo)
            def read = FileReaderCache.getInstance().getReader(name)
            def spectra = read.extractSpectraPixel(coo)
            if(spectra != null)
                aMap.put("spectra", spectra.getValues())
            else
                aMap.put("error", "Internal error try again")
        }
        catch(HDF5FileNotFoundException e){
            aMap.put("error", "File Not found")
        }
        catch(IndexOutOfBoundsException e){
            aMap.put("error", "Coordinates out of bounds")
        }
        catch(NumberFormatException e ){
            aMap.put("error", "Bad/null number format")
        }



        render aMap as JSON
    }


    @RestApiMethod(description="Get the spectra of  a rectangle", extensions = [".json"])
    @RestApiParams(params=[
            @RestApiParam(name="fif", type="String", paramType= RestApiParamType.QUERY, description="The absolute path of a multispectral image"),
            @RestApiParam(name="x", type="int", paramType= RestApiParamType.QUERY, description="The X coordinate" ),
            @RestApiParam(name="y", type="int", paramType= RestApiParamType.QUERY, description="The y coordinate" ),
            @RestApiParam(name="w", type="int", paramType= RestApiParamType.QUERY, description="The width of the rectangle" ),
            @RestApiParam(name="h", type="int", paramType= RestApiParamType.QUERY, description="The height of the rectangle" )

    ])
    def getSpectraRectangle(){
        def aMap = new HashMap()

        try{
            String name = params.fif
            def x = Integer.parseInt(params.x)
            def y = Integer.parseInt(params.y)
            def w = Integer.parseInt(params.w)
            def h = Integer.parseInt(params.h)
            def read = FileReaderCache.getInstance().getReader(name)
            def spectra = read.extractSpectraRectangle(x,y,w,h)
            def i = 0

            spectra.getValues().each { pxl ->
                def bMap = new HashMap()
                bMap.put("pixel", pxl[0])
                bMap.put("spectra", pxl[1])
                aMap.put(i, bMap)
                ++i
            }
        }
        catch(HDF5FileNotFoundException e){
            aMap.put("error", "File Not found")
        }
        catch(IndexOutOfBoundsException e){
            aMap.put("error", "Coordinates out of bounds")
        }
        catch (CacheTooSmallException e){
            aMap.put("error", "The figure is too big")
        }
        catch(NumberFormatException e ){
            aMap.put("error", "Bad/null number format")
        }

        render aMap as JSON
    }

}
