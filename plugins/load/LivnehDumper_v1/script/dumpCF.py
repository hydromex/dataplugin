#Multipurpose netCDF CF compliant dump script (some day)
#Handled cases, all of this for a single variable (could be multiple later on):
#Extract range of time and area from a single nc file (with time dimension)
#Extract area subset from a single nc file (no time dimension, assign the date of the file name or NA if not present)
#Extract range of time and area from multiple nc files in a given directory structure (with MFDataset as all have a time aggregation dimension)
#Extract range of time and area from multiple nc files in a given dir (no time dimension, so the dates must be in the file name, must specify the date format then)
#Case 1 and 2 can be treated the same as with case 3 and 4, respectively.
#Must be possible to specify a specific list of coordinates instead of a rectangle, althought they have to match exactly with the ones stored in the file
#Must get longitudes and latitudes ok regarding of the units (degrees_east, degrees_west, etc.)
#Must be possible to select specific days of the file by giving a list of dates, althought, as in the list of coordinates case, they must exactly match with the ones stored in the file.
#Other option could be export the data subset as another CF compliant nc file.
#Or the last option would be to throw all the data from all the dataset in ascii or database ready-to-digest format
#Make ranges inclusive or exclusive? Dump all data except a specific portion?
#Copy attributes to the output if nc file format is selected
#Possibility to specify an output structure... (Woah, not in this life!)
#Add elevation support?? ...no
#Specify date format??
#Specify coordinate point order?
#Remember that the MFDataset feature only works with the latest netCDF4 files, so if they are not in that version then, create a workaround
from netCDF4 import Dataset
from netCDF4 import MFDataset
from netCDF4 import num2date
from netCDF4 import date2num

import numpy as np
from os import path
import re

import argparse

import ncUtils as ncu

#Convenience method
def getLocIndxs(ncFile):
    #Validate case of list of points vs corners
    c1 = c2 = None
    if C1 and C2:
        c1 = ncu.parseCoord(C1)
        c2 = ncu.parseCoord(C2)

    return ncu.getLocationIndexes(ncFile, c1, c2)

def getTimeIndxs(ncFile):
    #Validate case of list of dates vs start and end date...
    st = ed = None
    if START_DATE and END_DATE:
        st = ncu.parseDate(START_DATE)
        ed = ncu.parseDate(END_DATE)
    
    return ncu.getPeriodIndexes(ncFile, st, ed)

#Write data to csv file row major wise (w. time index)
def writeToCSV(csvFile, varData, timeData, timeUnits, lats, lons, miss = False):

    for i, grid in enumerate(varData):
        #Compress to one dimension
        slats, slons, grid = ncu.compressGrid(lats, lons, grid, miss)
        dat = str(num2date(timeData[i], timeUnits))
        #Write to file
        for dataPoint in zip(slats, slons, [dat] * len(grid), grid):
            #csvFile.write(str(slats[j]) + ',' + str(slons[j]) + ',' + dat + ',' + str(dataPoint) + '\n')
            csvFile.write(','.join([str(e) for e in dataPoint]) + '\n')

#Write data to json file in features (w/o. time index)
def writeToJSON(jsonFile, varData, date, lats, lons, miss = False):
    
    #Compress to one dimension
    slats, slons, grid = ncu.compressGrid(lats, lons, varData, miss)
    #Write to file
    #GeoJSON accepts location in lon,lat format...
    for i, dataPoint in enumerate(zip(slons, slats, [str(date)] * len(grid), grid)):
        jsonData = '{"type": "Feature", "geometry": {"type": "Point", "coordinates": [' + str(dataPoint[0]) + ',' + str(dataPoint[1]) + \
        ']}, "properties": {"date": "' + dataPoint[2] + '", "' + VAR_NAME + '": ' + str(dataPoint[3]) + '}}'
        
        if i < len(grid) - 1:
            jsonData = jsonData + ','
            
        jsonFile.write(jsonData)
    #Finally, write the next line for the next file
    jsonFile.write('\n')

def writeToJSON_Mongo(jsonFile, varData, date, lats, lons, miss = False):
    
    #Compress to one dimension
    slats, slons, grid = ncu.compressGrid(lats, lons, varData, miss)
    #Write to file as a mongo document with an inner geojson for the location
    for dataPoint in zip(slons, slats, [str(date)] * len(grid), grid):
        jsonFile.write('{"location": {"type": "Point", "coordinates": [' + str(dataPoint[0]) + ',' + str(dataPoint[1]) + \
        ']}, "date": "' + dataPoint[2] + '", "' + VAR_NAME + '": ' + str(dataPoint[3]) + '}\n')

#Write data to csv file in row major and postgis WKT (w/o. time index)
def writeToCSVPG(csvFile, varData, date, lats, lons, miss = False):
    
    #Compress to one dimension
    slats, slons, grid = ncu.compressGrid(lats, lons, varData, miss)
    #Write to file (must put longitude first)
    for dataPoint in zip([str(date)] * len(grid), slons, slats, grid):
        csvFile.write(dataPoint[0] + ',POINT(' + str(dataPoint[1]) + ' ' + str(dataPoint[2]) + '),' + str(dataPoint[3]) + '\n')

#Write only metadata for netCDF file (dummy unused data just for load testing in rasdaman...)
def writeNCHeader(ncFile, lats, lons):
    #Must dimensions
    ncFile.createDimension(ncu.TIME_KEY, None)
    ncFile.createDimension(ncu.LAT_KEY, len(lats))
    ncFile.createDimension(ncu.LON_KEY, len(lons))
    #Dimension and data variables
    timesVar = ncFile.createVariable(ncu.TIME_KEY, 'f8', (ncu.TIME_KEY))
    lonsVar = ncFile.createVariable(ncu.LON_KEY, 'f8', (ncu.LON_KEY))
    latsVar = ncFile.createVariable(ncu.LAT_KEY, 'f8', (ncu.LAT_KEY))
    dataVar = ncFile.createVariable(VAR_NAME, 'f4', (ncu.TIME_KEY, ncu.LAT_KEY, ncu.LON_KEY))
    #Write data to variables
    lonsVar[:] = lons
    latsVar[:] = lats
    
    #Write dummy attributes
    ncFile.Conventions = 'CF-1.4'
    lonsVar.units = 'degrees_east'
    latsVar.units = 'degrees_north'
    #if not time dimension or transform current times to this value...
    timesVar.units = 'days since 1900-01-01 00:00:00'
    #dataVar.units = 'mm'

#Write to CF compliant netCDF file...
def writeToNC(ncFile, varData, date):
    timeVar = ncFile.variables[ncu.TIME_KEY]
    dataVar = ncFile.variables[VAR_NAME]
    
    #Get date as number...
    date = date2num(date, timeVar.units)
    #Write new date and grid to file
    lastIdx = len(timeVar)
    timeVar[lastIdx] = date
    dataVar[lastIdx] = varData

#Validate if all the files can be opened as a MFDataset
def isPartOfMFDataset(ncFile):
    ok = True
    with Dataset(ncFile, 'r') as ncF:
        if not ncF.file_format.startswith('NETCDF3_') and ncF.file_format != 'NETCDF4_CLASSIC' or ncu.TIME_KEY not in ncF.variables:
            ok = False
    return ok

def getFileDate(fileName):
    #Would be best if provided some kind of format or regex to exactly match the format of the date in the file name
    #Regex not generic (just works for TRMM data)
    dat = re.match(r'^.+\.(\d{10})', fileName).group(1)
    return ncu.parseDate('-'.join([dat[:4], dat[4:6], dat[6:8]]) + 'T' + dat[8:])

#Create a function that reads a file for parsing a list of coordinate points (lat, lon)

#Create a function that reads a file for parsing a list of dates (yyyy-mm-dd)

#Main
#Default configuration: Dump row-wise like lat, lon, date, var
#Lats: North, West.
#If no time dimension get the time from the file name, else get it from the time dimension itself.
parser = argparse.ArgumentParser(description = 'Dump data of netCDF CF compliant file to ascii or netCDF')
parser.add_argument('data', type = str, help = 'file or directory for the netCDF input data')
parser.add_argument('varName', type = str, help = 'name of variable to dump (or list of variable names separated by comma)')
parser.add_argument('--c1', dest = 'coord1', help = 'upper left corner of subarea to dump')
parser.add_argument('--c2', dest = 'coord2', help = 'lower right corner of subarea to dump')
parser.add_argument('--coords', dest = 'cPoints', help = 'file with a list of coordinates of the data to dump')
parser.add_argument('--stDate', dest = 'startDate', help = 'start date of data to dump')
parser.add_argument('--edDate', dest = 'endDate', help = 'end date of data to dump')
parser.add_argument('--dates', dest = 'dates', help = 'file with a list of dates of the data to dump')
parser.add_argument('-f', dest = 'outputFormat', help = 'The format for the output from {csv, geojson, json-mongo, nc, csv-pg}, default: CSV.')
parser.add_argument('-o', dest = 'outputFile', help = 'The file where to store all the dumped data')

FMT_CSV = 'csv'
FMT_GEOJSON = 'geojson'
FMT_MONGO = 'json-mongo'
FMT_NC = 'nc'
FMT_PG = 'csv-pg'

args = parser.parse_args()

DATA = args.data
VAR_NAME = args.varName

C1 = args.coord1
C2 = args.coord2
COORDS = args.cPoints

START_DATE = args.startDate
END_DATE = args.endDate
DATES = args.dates

FORMAT = args.outputFormat

OUTPUT_FILE = args.outputFile or 'CFDumpedData'
if FORMAT is None or FORMAT.lower() == FMT_CSV:
    if not re.match(r'^.*\.csv$', OUTPUT_FILE, re.I):
        OUTPUT_FILE = OUTPUT_FILE + '.csv'
    FORMAT = FMT_CSV
elif FORMAT.lower() == FMT_GEOJSON or FORMAT.lower() == FMT_MONGO:
    if not re.match(r'^.*\.json$', OUTPUT_FILE, re.I):
        OUTPUT_FILE = OUTPUT_FILE + '.json'
elif FORMAT.lower() == FMT_NC:
    if not re.match(r'^.*\.nc4?$', OUTPUT_FILE, re.I):
        OUTPUT_FILE = OUTPUT_FILE + '.nc'
elif FORMAT.lower() == FMT_PG:
    if not re.match(r'^.*\.csv$', OUTPUT_FILE, re.I):
        OUTPUT_FILE = OUTPUT_FILE + '_pg.csv'
else:
    print('Invalid option!!')
    exit(-1)

latIndexes = lonIndexes = timeIndexes = None
#Validate the input arguments to select the right case.
if path.isdir(DATA):
    #Means want to extract from a bunch of files, get list of files contained in it
    print('multi file mode...')
    ncFiles = ncu.getNCFiles(DATA)
    #See if all the files (only the first, lets be optimist) can be opened with MFDataset (NC4 and time dim), else
    #Open one by one.
    if isPartOfMFDataset(ncFiles[0]):
        print('Opening all files as MFDataset...')
        #This time it is a set of files that have a time dimension and are in a supported netCDF version.
        #Open all the netCDF source files as one dataset:
        with MFDataset(ncFiles) as mds:
            latIndexes, lonIndexes = getLocIndxs(mds)
            lats = mds.variables[ncu.LAT_KEY][latIndexes]
            lons = mds.variables[ncu.LON_KEY][lonIndexes]
            
            timeIndexes = getTimeIndxs(mds)
            timeVar = mds.variables[ncu.TIME_KEY]
            timeUnits = timeVar.units
            
            dataVar = mds.variables[VAR_NAME]
            
            if(timeIndexes is not None):
                #Get the date values and indexes ordered by date (not ordered by default)
                times = sorted(list(zip(timeVar[timeIndexes], timeIndexes)))
                
                #Now save the subset data, do it one date at a time to stop memory overflow
                if FORMAT == FMT_NC:
                    #Create file compatible with rasdaman...
                    with Dataset(OUTPUT_FILE, 'w', format = 'NETCDF4_CLASSIC') as ncDSFile:
                        writeNCHeader(ncDSFile, lats, lons)
                        #Write the data...
                        for timeStep in times:
                            varData = dataVar[timeStep[1]][latIndexes][:, lonIndexes]
                            #Write data to nc file...
                            writeToNC(ncDSFile, varData, num2date(timeStep[0], timeUnits))
            else:
                #This may never happen, except for bad parsed date...
                print('Can\'t extract time data, bad date format!')
            
            
            #Once the dates and location (indexes) are set, start writing the data to the file...
    else:
        print('Files not eligible for MFDataset, opening one by one...')
        #This could be that the files dont have a time dimension (then the date must be on the file name)
        #or that the files are not in a supported version of netCDF (and could have time dimension anyways)
        #Lets asume that the dates are in the file name then
        ncFiles = [(getFileDate(f), f) for f in ncFiles]
        #Filter files to get only the wanted period
        if START_DATE and END_DATE:
            ncFiles = [f for f in ncFiles if f[0] >= ncu.parseDate(START_DATE)
            and f[0] <= ncu.parseDate(END_DATE)]
        #else select files from wanted date list
        
        ncFiles = sorted(ncFiles)
        #Could be more specific (if for some case there are also a time dimension, but not now)
        #Now get the indexes of the area wanted to extract
        latIndexes = lonIndexes = None
        lats = lons = None
        with Dataset(ncFiles[0][1], 'r') as ncFile:
            latIndexes, lonIndexes = getLocIndxs(ncFile)
            lats = ncFile.variables[ncu.LAT_KEY][latIndexes]
            lons = ncFile.variables[ncu.LON_KEY][lonIndexes]
        
        #Once the dates (files) and location (indexes) are set, start writing the data to the file...
        if FORMAT == FMT_CSV:
            print('Writing to csv!...')
        elif FORMAT == FMT_GEOJSON:
            print('Writing as geojson file!!...')
            #Truncate output file and add the header
            with open(OUTPUT_FILE, 'w') as jsonFile:
                jsonFile.write('{\n"type": "FeatureCollection",\n"features": [\n')
            
            #Write all the features...
            with open(OUTPUT_FILE, 'a') as jsonFile:
                for ncFile in ncFiles:
                    with Dataset(ncFile[1], 'r') as ncF:
                        varData = ncF.variables[VAR_NAME][latIndexes, lonIndexes]
                        #Write data to json, sending the date representing this file
                        writeToJSON(jsonFile, varData, ncFile[0], lats, lons)
            
            #Finally close the json object at the end
            with open(OUTPUT_FILE, 'a') as jsonFile:
                jsonFile.write(']\n}')
        elif FORMAT == FMT_MONGO:
            print('Writing as json file for mongo import!!...')
            #Truncate output file and add the header
            with open(OUTPUT_FILE, 'w') as jsonFile:
                #Just truncate the file
                jsonFile.write('')
            
            #Write all the records...
            with open(OUTPUT_FILE, 'a') as jsonFile:
                for ncFile in ncFiles:
                    with Dataset(ncFile[1], 'r') as ncF:
                        varData = ncF.variables[VAR_NAME][latIndexes, lonIndexes]
                        #Write data to json, sending the date representing this file
                        writeToJSON_Mongo(jsonFile, varData, ncFile[0], lats, lons)
                        
        elif FORMAT == FMT_PG:
            #Before starting writing, truncate file
            with open(OUTPUT_FILE, 'w') as csvFile:
                csvFile.write('date,loc,' + VAR_NAME + '\n')

            with open(OUTPUT_FILE, 'a') as csvFile:
                for ncFile in ncFiles:
                    with Dataset(ncFile[1], 'r') as ncF:
                        varData = ncF.variables[VAR_NAME][latIndexes, lonIndexes]
                        #Write data to json, sending the date representing this file
                        writeToCSVPG(csvFile, varData, ncFile[0], lats, lons)
        elif FORMAT == FMT_NC:
            #Create file compatible with rasdaman...
            with Dataset(OUTPUT_FILE, 'w', format = 'NETCDF3_CLASSIC') as ncDSFile:
                writeNCHeader(ncDSFile, lats, lons)
                
                for ncFile in ncFiles:
                    with Dataset(ncFile[1], 'r') as ncF:
                        varData = ncF.variables[VAR_NAME][latIndexes, lonIndexes]
                        #Write data to nc file...
                        writeToNC(ncDSFile, varData, ncFile[0])
        
elif path.isfile(DATA):
    #Only extract from a single file then...
    print('single file mode...')
    #Get the static location indexes, send the only file
    with Dataset(DATA, 'r') as ncFile:
        #Get location indexes
        latIndexes, lonIndexes = getLocIndxs(ncFile)
        #Now get time indexes...
        if START_DATE and END_DATE:
            #Get range of dates
            timeIndexes = getTimeIndxs(ncFile)
            if timeIndexes is None:
                #It is either empty or no time dimension is there, may also be a problem parsing the date...
                #First try other things if there is the date on the file or a format were specified.. blah
                print('could not extract time indexes...')
        #else do list of dates extraction
        #else get all the time period (bad idea)
        
        #Validate here if the variable name is just a name or a list of names...
        #For now lets keep it simple and be just one name
        #Have to validate if the output format will be nc or csv, just save to csv for now
        #For each date in all variable dates, save the grid to csv
        varData = ncFile.variables[VAR_NAME][timeIndexes, latIndexes, lonIndexes]
        lats = ncFile.variables[ncu.LAT_KEY][latIndexes]
        lons = ncFile.variables[ncu.LON_KEY][lonIndexes]
        timeUnits = ncFile.variables[ncu.TIME_KEY].units
        timeData = ncFile.variables[ncu.TIME_KEY][timeIndexes]
        
        if (FORMAT == FMT_CSV):
            #Before starting writing, truncate file
            with open(OUTPUT_FILE, 'w') as csvFile:
                csvFile.write(ncu.LAT_KEY + ';' + ncu.LON_KEY + ';date;' + VAR_NAME + '\n')

            with open(OUTPUT_FILE, 'a') as csvFile:
                writeToCSV(csvFile, varData, timeData, timeUnits, lats, lons)