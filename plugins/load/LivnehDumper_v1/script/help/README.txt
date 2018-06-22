usage: [data input] [nc variable] [start date] [end date]

where [data input] is the file or directory for the netCDF input data,
[nc variable] is the name of 'in-file' netCDF variable to extract the data from
[start date] is the date in the format of yyyymmdd from where to subset the data in the temporal dimension.
[end date] is the date in the format of yyyymmdd to where to subset the data in the temporal dimension.

[start date] and [end date] are optional parameters

Examples:
Get the data of the 'precipitation' variable from January 1st, 2013 to March 3rd, 2015, from the ncs dir.
ncs precipitation 20130101 20150303

Get the data of the 'uncal_precipitation' variable from December 20th, 2000 to July 5th, 2010, from the ncs dir.
ncs uncal_precipitation 20001220 20100705

Get the data from the 'Prec' variable for all the times available in the dataset, from the gpm.nc file

gpm.nc Prec