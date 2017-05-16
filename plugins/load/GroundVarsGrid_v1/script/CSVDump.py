from netCDF4 import Dataset
from numpy import where
from numpy import savetxt
from numpy import ma
from sys import argv

from os.path import basename
from random import randint

#Get arguments from command-line (nc file to save to csv)
ncFile = argv[1]
csvFile = argv[2]

#Open ncFile
ds = Dataset(ncFile)
#Get name of netCDF variable from fileName
varName = basename(ncFile).split('.nc')[0][:-1]

#load random piece of grid of random depth
pieceSize = (ds.variables['lat'][:].size)/32

depth = randint(0, 3)
latPiece = randint(0, 31)
print('selected lat piece: ' + str(latPiece) + ' depth: ' + str(depth) + '\n')
data = ds.variables[varName][depth, (pieceSize * latPiece):(pieceSize * (latPiece + 1)), :]
#load lats and lons of one depth
lons = ds.variables['lon'][:]
lats = ds.variables['lat'][(pieceSize * latPiece):(pieceSize * (latPiece + 1))]

#Remove missing values
#Validate if it is a masked array or not
if isinstance(data, ma.MaskedArray):
    #If the result is a masked array compress the data to 1D without missing values
    #Get indexes of compressed data
    (latIndexes, lonIndexes) = where(data.data != data.fill_value)
    #Compress data
    data = data.compressed()
else:
    #The result is a numpy.ndarray (no missing values), only flatten to 1D
    #Get all indexes of lats and lons
    (latIndexes, lonIndexes) = where(data)
    #Flatten data
    data = data.flatten()

#Get values of all indexes from lats and lons
lats = lats[latIndexes[:]]
lons = lons[lonIndexes[:]]
    
del latIndexes, lonIndexes

depth = ds.variables['depth'][depth]

#Send array data to a file
savetxt(csvFile, zip([depth] * data.size, lats, lons, data), fmt = '%g,%g,%g,%g', header = 'depth;lat;lon;' + varName)