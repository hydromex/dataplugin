usage: python3 CSVDump.py [inputDirectory] [outputDirectory]
Where [inputDirectory] = the directory where the data and syntax files are stored. The syntax files must be in .sps or .spss format
[outputDirectory] = the directory where to store the generated CSV dump files. It will contain a subfolder for each dataset
in the [inputDirectory] and a sql instructions file to be executed by the psql command to load all the data in the CSVs.