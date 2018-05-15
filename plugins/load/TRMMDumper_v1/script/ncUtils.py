from netCDF4 import Dataset
from netCDF4 import datetime
from netCDF4 import date2num
import matplotlib.pyplot as plt
import cartopy.crs as ccrs
import cartopy.feature as cfeat
from numpy import where
from numpy import mean
from numpy.ma import MaskedArray

from os import path
from os import walk
import re

#Constants
LAT_KEY = 'lat'
LON_KEY = 'lon'
TIME_KEY = 'time'

#DEGREES_N = 'degrees_north'
#DEGREES_E = 'degrees_east'
#DEGREES_S = 'degrees_south'
#DEGREES_W = 'degrees_west'

def plotFile(fname, varName, c1 = None, c2 = None, dates = None):
    if c1 and c2:
        print('Coords ===> c1: ' + str(c1[0]) + ',' + str(c1[1]) + ' c2: ' + str(c2[0]) + ',' + str(c2[1]))
    
    with Dataset(fname, 'r') as ds:
        #Get x,y and variable data
        lats = ds.variables[LAT_KEY][:]
        lons = ds.variables[LON_KEY][:]
        var = ds.variables[varName]
        varUnits = var.units
        varLongName = ''
        plotTitle = 'Dataset: ' + fname + ' Variable: '
        
        for attr in var.ncattrs():
            #Because it seems there is not a standarized way of naming long_name attr
            #Maybe latter match this with a regex
            if 'name' in attr.lower() and 'long' in attr.lower():
                varLongName = var.getncattr(attr)
        
        plotTitle += varLongName
        
        #Have to see if there are times or not...
        if TIME_KEY in ds.variables:
            #Plot one time or all averaged or a range of dates averaged...
            print('with time...')
            if not dates:
                #Default average...
                varData = mean(var[:], axis = 0)
                if len(var) > 1:
                    #Means more than one time
                    plotTitle += ', averaged time series'
            else:
                print('dates selected: start -> ' + str(dates[0]) + ' end -> ' + str(dates[1]))
                #Get wanted date range...
                times = ds.variables[TIME_KEY]
                #Start date
                stDateIdx = where(times[:] == date2num(dates[0], times.units))[0][0]
                #End date
                edDateIdx = where(times[:] == date2num(dates[1], times.units))[0][0]
                
                #Happy path, must later validate if this and also the rect area up is correct. Also if st < ed
                varData = mean(var[stDateIdx:edDateIdx + 1], axis = 0)
                plotTitle += ', averaged time from ' + str(dates[0]) + ' to ' + str(dates[1])
        else:
            #If no time just get the data grid
            varData = var[:]
        
        #Select subset if wanted...
        if c1 and c2:
            lats, lons, varData = getRectData(lats, lons, varData, c1, c2)
        
        #plot
        plotContourMap(lons, lats, varData, varUnits, plotTitle)


def plotContourMap(lons, lats, data, cbarLbl, title):
    #Create map plot
    print('plotting...')
    ax = plt.axes(projection = ccrs.PlateCarree())
    ax.coastlines()
    ax.add_feature(cfeat.BORDERS)
    
    gl = ax.gridlines(ccrs.PlateCarree(), True)
    gl.xlabels_top = False
    gl.ylabels_right = False
    plot = ax.contourf(lons, lats, data, 60, transform = ccrs.PlateCarree())
    plt.colorbar(plot, orientation = 'horizontal').set_label(cbarLbl)
    plt.title(title)
    #plt.tight_layout()
    
    #Show map
    plt.show()

    
def getRectData(lats, lons, data, c1, c2):
    #Get data subset. Coordinate points in form of 0: lat, 1: lon
    #Get indexes
    print('Getting area from ' + str(c1) + ' to ' + str(c2))
    latRange, lonRange = getRectCoordIndexes(lats, lons, c1, c2)
    
    #Get subset data
    lats = lats[latRange]
    lons = lons[lonRange]
    data = data[latRange][:, lonRange]
    
    print('Area data extracted...')
    
    return (lats, lons, data)
    
def getRectCoordIndexes(lats, lons, c1, c2):
    latRange = where((lats >= min(c1[0], c2[0])) & (lats <= max(c1[0], c2[0])))[0]
    lonRange = where((lons >= min(c1[1], c2[1])) & (lons <= max(c1[1], c2[1])))[0]
    
    return (latRange, lonRange)

#Flatten to 1D without missing values, or with them if specified
def compressGrid(lats, lons, grid, miss = False):
    #It has missing values...
    if isinstance(grid, MaskedArray) and not miss:
        (latIndexes, lonIndexes) = where(grid.data != grid.fill_value)
        #Compress data
        grid = grid.compressed()
    else:
        #No missing values, just flatten... (but must be a ndarray)
        (latIndexes, lonIndexes) = where(grid == grid)
        grid = grid.flatten()
        #If masked array then get the missing vals instead of --
        if isinstance(grid, MaskedArray):
            grid = grid.data
        
    lats = lats[latIndexes]
    lons = lons[lonIndexes]
    
    return (lats, lons, grid)

def parseCoord(coord):
    return [float(c) for c in coord.split(',')]

#Need to validate also...
def parseDate(dat):
    dateRe = r'^(\d{4})(?:-|\.|\/)?(0[1-9]|1[0-2])(?:-|\.|\/)?(0[1-9]|[1-2]\d|3[01])(?:T?([01]\d|2[0-3])(?::?([0-5]\d)(?::?([0-5]\d))?)?)?$'
    
    parDat = re.match(dateRe, dat)
    if parDat:
        parDat = [int(e) for e in parDat.groups(default = 0)]
        #If it is efectively a date
        return datetime(*parDat)
    else:
        return None

#Add a function that walks through all the directory structure of a folder and returns all the
#nc files in there, or also the list of optional file extensions
def getNCFiles(folder, ext = None):
    extRe = r'^.+\.(:?nc|nc4)$'
    extraExt = r'^.+\.(:?' + '|'.join(ext) + ')$' if ext else r'NO_MATCH'
    
    if not path.isdir(folder):
        return None
    
    ncFiles = []
    for currDir, _, files in walk(folder):
        #Map list of files to key, value where key is the day and value is the file name...
        ncFiles += [path.join(currDir, f) for f in files if re.match(extRe, f, re.IGNORECASE)
        or re.match(extraExt, f, re.IGNORECASE)]
    
    return ncFiles

#def coordUnits(units):
    #degNRe = r'degrees?(?:_north|_?N)'
    #degERe = r'degrees?(?:_east|_?E)'
    #degSRe = r'degrees?(?:_south|_?S)'
    #degWRe = r'degrees?(?:_west|_?W)'
    
    #if re.match(degNRe, units, re.IGNORECASE):
        #return DEGREES_N
    #elif re.match(degERe, units, re.IGNORECASE):
        #return DEGREES_E
    #elif re.match(degSRe, units, re.IGNORECASE):
        #return DEGREES_S
    #elif re.match(degWRe, units, re.IGNORECASE):
        #return DEGREES_W
    #else
        #return None
    
#Add a function that recieves an open ncFile and gets its lat and lon indexes for a given rectangle or list of latitudes.
#Transforming correctly between units and so on

#Validate if degrees east are 0 to 360 or -180 to 180 (default)
#def transformCoords(coordPoints, fromUnits, toUnits):
    ##If the units are the same then get the indexes
    #if fromUnits == toUnits:
        ##But before transform any > 180 to negative.
        #coordPoints = np.array(coordPoints)
        #coordPoints[coordPoints > 180] - 360
        #return coordPoints
    
    #if fromUnits == DEGREES_W and toUnits == DEGREES_E:
        #return 360 - coordPoints
    ##Later do the transformation for degrees north
    #else:
        ##Trying something invalid, return none
        #return None

def getLocationIndexes(ncFile, c1 = None, c2 = None, lats = None, lons = None):
    #Assuming lat = degrees north and lon = degrees east
    
    if type(ncFile) is not Dataset:
        return None
    #Retrieve lats and lons indexes according to the given units and the file units
    latVar = ncFile.variables[LAT_KEY]
    lonVar = ncFile.variables[LON_KEY]
    
    #Units must be degrees east for both
    if c1 and c2:
        print('getting location range')
        #If range wanted then get the lat and lon indexes range
        return getRectCoordIndexes(latVar[:], lonVar[:], c1, c2)
    #elif lats and lons #(for list of points some day)
    else:
        #No selection of anything...
        return (list(range(len(latVar))), list(range(len(lonVar))))

#Do the same to get the indexes of a range of dates or a start and end date (datetime)
def getPeriodIndexes(ncFile, stDate = None, edDate = None, dates = None):
    #The dates must be in yyyy-mm-dd format!
    
    if type(ncFile) is not Dataset or TIME_KEY not in ncFile.variables:
        return None
    
    timeVar = ncFile.variables[TIME_KEY]
    
    if stDate and edDate:
        #Range of dates...
        print('getting date range...')
        return where((timeVar[:] >= date2num(stDate, timeVar.units)) & (timeVar[:] <= date2num(edDate, timeVar.units)))[0]
    #else dates: #for list of dates some day