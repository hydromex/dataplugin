from sys import argv
from collections import OrderedDict
import csv
import os
import re
from time import time
from shutil import rmtree
from copy import deepcopy

#SCRIPT ARGUMENTS (IN Sintax and data dir, OUT csv and sql file dir)
INPUT_DIR = argv[1] if argv[1][-1] == '/' else argv[1] + '/'
OUTPUT_DIR = argv[2] if argv[2][-1] == '/' else argv[2] + '/'
#CONSTANTS
SQL_FILE = OUTPUT_DIR + 'loadDatabases.sql'
#Cause a variable can be named the same as a reserved word...
PSQL_R_W = {'select', 'all', 'analyze', 'any', 'and', 'as', 'asc', 'both',
'case', 'check', 'column', 'default', 'desc', 'distinct', 'do', 'end', 'group', 'limit', 
'new', 'offset', 'old', 'order', 'table', 'user'}

#Regexes...
DATAFILE_RE = re.compile(r'.*(?:"|\').*\\(.*\.txt)(?:"|\')', re.I)
VARS_POS_RE = re.compile(r'^\s*\/?\s*\b(\w+)\s+(\d+)\s*-\s*(\d+)\s*(?:\((a|f,\d)\))?', re.I)
COMMAND_END_RE = re.compile(r'^\s*\.\s*$')
VARS_LABEL_RE = re.compile(r'^\s*(\w+)\s+("[^"]+"|\'[^\']+\')')
VARS_RANGE_RE = re.compile(r'^\s*(?:\b[^\d\s]\w*\b\s*)+')
VALS_ARR_RE = re.compile(r'^\s*(\(\s*(?:(?:\'|")?\w+(?:\'|")?\s*,?\s*)+\s*\))')
DECL_END_RE = re.compile(r'^\s*\/')
ID_LABEL_RE = re.compile(r'^\s*("?[^"\s]+"?|\'?[^\'\s]+\'?)\s+("[^"]+"|\'[^\']+\')')
RECODE_CMD_RE = re.compile(r'^\s*recode\s+(.+)(\((?:\w+=\w+)+\))', re.I)
VAR_LABELS_RE = re.compile(r'^\s*var(?:iable)?\s*lab(?:el)?s?', re.I)
VAL_LABELS_RE = re.compile(r'^\s*val(?:ue)?\s*lab(?:el)?s?', re.I)
MISS_VALS_RE = re.compile(r'^\s*miss(?:ing)?\s*val(?:ue)?s?', re.I)
BAD_CHARS_RE = re.compile(r'[^\wä-ü\t\x20-\x7e]', re.I)
FC_VAR06_RE = re.compile(r'^\s*full\s+credit\s*$', re.I)

#keys
TYPE_KEY = 'type'
LABEL_KEY = 'label'
VALUE_LABELS_KEY = 'valueLabels'
VAL_DICT_KEY = 'valDict'
NAME_KEY = 'name'
MISSING_VALUES_KEY = 'missingValues'
START_KEY = 'start'
END_KEY = 'end'
FOREIGN_KEY = 'foreign'
RECODE_KEY = 'recode'

#types
NUMERIC_TYPE = 'numeric'
DOUBLE_TYPE = 'double precision'
INT_TYPE = 'integer'
TEXT_TYPE = 'text'

#Sintax commands
VAR_LABELS = 'VAR LABELS'
VAL_LABELS = 'VAL LABELS'
MISS_VALS = 'MISS VALS'

ENCODING = 'utf-8'


#FUNCTIONS
def createDir(path):
    if not os.path.exists(path):
        os.makedirs(path)

def deleteDir(path):
    if os.path.exists(path):
        rmtree(path)

def clearText(txt):
    txt = txt.strip()
    if txt[0] == '"' or txt[0] == "'":
        txt = txt[1:-1].strip()
    #Remove invalid chars
    txt = re.sub(BAD_CHARS_RE, '', txt)
    
    return txt

def getVar(varName):
    return clearText(varName).lower()

def getValueSet(txt):
    #Remove parenthesis and split by comma and save without quotes
    return set(clearText(val) for val in re.split(r'\s*,\s*', txt[1:-1]))

def createAuxTableSQL(varName, fileName):
    global createStmts
    global tableCommentStmts
    global copyDataStmts
    global addKeysStmts
    global PSQL_R_W
    
    tableName = varName if varName not in PSQL_R_W else '"' + varName + '"'
    #Create table with no indexes
    createStmts += 'CREATE TABLE ' + tableName + '( id ' + varsDict[varName][TYPE_KEY] + ', descr text);\n'
    #Add table comment, escape ' character
    tableCommentStmts += "COMMENT ON TABLE " + tableName + " IS '" + varsDict[varName][LABEL_KEY].replace("'", "''") + "';\n"
    #Copy data from csv
    copyDataStmts += '\COPY ' + tableName + " FROM '" + fileName + "' WITH DELIMITER ',' CSV HEADER;\n"
    #Create primary key
    addKeysStmts += 'ALTER TABLE ' + tableName + ' ADD PRIMARY KEY (id);\n'

def saveToCSV(csvFilename, fieldNames, dataDict):
    with open(csvFilename, 'w', newline = '', encoding = ENCODING) as f:
        writer = csv.DictWriter(f, fieldNames, lineterminator = '\n')
        writer.writeheader()
        writer.writerows(dataDict)
    
def setValueLabels(varRange, valueLabels, valuesDict):
    currVars = varRange.lower().split()
    i = 0
    while i < len(currVars) - 1:
        #Save all current value labels in each variable of the current range
        if currVars[i + 1] != 'to':
            varsDict[currVars[i]][VALUE_LABELS_KEY] = deepcopy(valueLabels)
            varsDict[currVars[i]][VAL_DICT_KEY] = deepcopy(valuesDict)
        else:
            #We have a var range, proceed with the variables search
            #Save all the variables except the last one of the range (non inclusive)
            varNames = list(varsDict)   #Get a list of the dictionary keys
            for var in varNames[varNames.index(currVars[i]):varNames.index(currVars[i + 2])]:
                varsDict[var][VALUE_LABELS_KEY] = deepcopy(valueLabels)
                varsDict[var][VAL_DICT_KEY] = deepcopy(valuesDict)
            i = i + 1
        i = i + 1
    varsDict[currVars[i]][VALUE_LABELS_KEY] = deepcopy(valueLabels)
    varsDict[currVars[i]][VAL_DICT_KEY] = deepcopy(valuesDict)

def setMissingValues(varRange, missingValues):
    currVars = varRange.lower().split()
    i = 0
    while i < len(currVars) - 1:
        #Save all current value labels in each variable of the current range
        if currVars[i + 1] != 'to':
            varsDict[currVars[i]][MISSING_VALUES_KEY] = missingValues
        else:
            #We have a var range, proceed with the variables search
            #Save all the variables except the last one of the range (non inclusive)
            varNames = list(varsDict)   #Get a list of the dictionary keys
            for var in varNames[varNames.index(currVars[i]):varNames.index(currVars[i + 2])]:
                varsDict[var][MISSING_VALUES_KEY] = missingValues
            i = i + 1
        i = i + 1
    varsDict[currVars[i]][MISSING_VALUES_KEY] = missingValues

def setRecodeOp(varRange, recode):
    currVars = varRange.lower().split()
    i = 0
    while i < len(currVars) - 1:
        #Save recode operation in each variable of the current range
        if currVars[i + 1] != 'to':
            varsDict[currVars[i]][RECODE_KEY] = recode
        else:
            varNames = list(varsDict)   #Get a list of the dictionary keys
            for var in varNames[varNames.index(currVars[i]):varNames.index(currVars[i + 2])]:
                varsDict[var][RECODE_KEY] = recode
            i = i + 1
        i = i + 1
    varsDict[currVars[i]][RECODE_KEY] = recode

def isForeignKey(varName):
    
    if FOREIGN_KEY in varsDict[varName]:
        return varsDict[varName][FOREIGN_KEY]
        
    if VALUE_LABELS_KEY in varsDict[varName]:
        #If the variable has value labels declared
        if MISSING_VALUES_KEY in varsDict[varName]:
            #If the variable has missing values declared
            missVals = varsDict[varName][MISSING_VALUES_KEY]
            #Get first value id
            valId = list(varsDict[varName][VAL_DICT_KEY].keys())[0]
            #Verify only that the first value is a missing value
            varsDict[varName][FOREIGN_KEY] = valId not in missVals
        else:
            #If no missing values then it is an aux table
            varsDict[varName][FOREIGN_KEY] = True
        
        #Post validation of 2006 Data, where its considered as FK but with nothing declared...
        #Validate 2006 data rule, if just one label matches and more than 2 length...
        if varsDict[varName][END_KEY] - varsDict[varName][START_KEY] > 0:
            #Means variable size is bigger than 2, and that is when things go hairy
            for e in list(varsDict[varName][VAL_DICT_KEY].values()):
                if FC_VAR06_RE.match(e):
                    #Its a type of variable with 'full credit', so is probable to add a lot of unnecesary registries if not disabled FK
                    varsDict[varName][FOREIGN_KEY] = False
                    #LOG
                    print ('variable ' + varName + ' FK turned off')
                    #LOG
                    break
    else:
        #If no value labels entry then its not an aux table
        varsDict[varName][FOREIGN_KEY] = False
    
    return varsDict[varName][FOREIGN_KEY]

dumpStartTime = time()
#Get syntax files...
SYNTAX_FILES = []
for f in os.listdir(INPUT_DIR):
    if re.match(r'.+\.spss?$', f):
        SYNTAX_FILES.append(INPUT_DIR + f)

#Remove previous data directory if exists
deleteDir(OUTPUT_DIR)

for SYNTAX_FILENAME in SYNTAX_FILES:
    dsStartTime = time()
    #DATASET VARIABLES
    DATABASE = ''
    syntaxCommand = ''
    currentVar = ''
    #metadata
    varsDict = OrderedDict()
    #value labels
    currValLabels = []
    currValsDict = OrderedDict()
    #SQL instructions
    createStmts = ''
    tableCommentStmts = ''
    copyDataStmts = ''
    addKeysStmts = ''

    #missing values
    currMissVals = {}

    print (' <<< START OF DATASET >>>')
    print ('Reading syntax file [' + SYNTAX_FILENAME + ']...')
    #Measure execution time of the whole dataset processing
    startTime = time()
    with open(SYNTAX_FILENAME, encoding = ENCODING) as syntaxFile:

        for line in syntaxFile:
            if DATAFILE_RE.match(line):
                #data file location
                dataFilename =  DATAFILE_RE.match(line).group(1)
                DATABASE = dataFilename.replace('.txt', '').lower()
                CSV_DIR = OUTPUT_DIR + DATABASE + '/'
                print ('Creating CSV output directory... [' + CSV_DIR + ']')
                createDir(CSV_DIR)
                #Write first sql instructions of sql file
                with open(SQL_FILE, 'a', encoding = ENCODING) as sqlFile:
                    sqlFile.write('CREATE DATABASE ' + DATABASE + ';\n\c ' + DATABASE + ';\nBEGIN TRANSACTION;\n')
            elif VARS_POS_RE.match(line):
                m = VARS_POS_RE.match(line)
                varName = getVar(m.group(1))
                varType = m.group(4).lower() if m.group(4) else m.group(4)
                #Infer the correct type
                varType = NUMERIC_TYPE if not varType else TEXT_TYPE if varType == 'a' else DOUBLE_TYPE if varType.split(',')[1] != '0' else INT_TYPE
                #Save variable position in dict
                varsDict[varName] = {START_KEY: int(m.group(2)), END_KEY: int(m.group(3)), TYPE_KEY: varType}
            elif syntaxCommand == VAR_LABELS or VAR_LABELS_RE.match(line):
                if not syntaxCommand:
                    syntaxCommand = VAR_LABELS
                    line = line.replace(VAR_LABELS_RE.match(line).group(), '', 1)
                #In var labels command
                if line.strip():
                    m = VARS_LABEL_RE.match(line)
                    if m:
                        varName = getVar(m.group(1))
                        varLabel = clearText(m.group(2))
                        #Save variable label in the dictionary
                        varsDict[varName][LABEL_KEY] = varLabel
                        line = line.replace(m.group(), '')
                    
                    if COMMAND_END_RE.match(line):
                        #In the last line of var labels command
                        syntaxCommand = ''
            elif syntaxCommand == VAL_LABELS or VAL_LABELS_RE.match(line):
                if not syntaxCommand:
                    syntaxCommand = VAL_LABELS
                    line = line.replace(VAL_LABELS_RE.match(line).group(), '', 1)
                    
                #In value labels command
                while line.strip():
                    #First validate if not ending a previous declaration
                    m = DECL_END_RE.match(line)
                    if m:
                        #Means that the end of the current variable is reached, but not the end of the value labels command
                        
                        #If no current variable then that means its the first variable declared for some reason with an innecesary / first
                        if currentVar:
                            #Save value labels in all the variables in the current range
                            setValueLabels(currentVar, currValLabels, currValsDict)
                            #Clean all temp variables
                            currValLabels = []
                            currValsDict = OrderedDict()
                            currentVar = ''
                        line = line.replace(m.group(), '', 1) #Remove decl end /
                    
                    #Add or append new line to currentVar if a variable range
                    m = VARS_RANGE_RE.match(line)
                    if m:
                        currentVar += ' ' + m.group().strip()
                        line = line.replace(m.group(), '', 1) #Remove current matched var range from line
                        #Transform \n to \s
                        currentVar = getVar(currentVar.replace('\n', ' '))
                    
                    #If in value labels and current variable is enabled.
                    #split id and value (or tag) until the line has no more entries
                    m = ID_LABEL_RE.match(line)
                    while m:
                        valueName = clearText(m.group(1))
                        valueLabel = clearText(m.group(2))
                        #Save value label in curr val labels list
                        currValLabels.append({NAME_KEY: valueName, LABEL_KEY: valueLabel})
                        #Save value label in dictionary
                        currValsDict[valueName] = valueLabel
                        line = line.replace(m.group(), '', 1)
                        m = ID_LABEL_RE.match(line)
                    
                    m = COMMAND_END_RE.match(line)
                    if m:
                        if currentVar:
                            setValueLabels(currentVar, currValLabels, currValsDict)
                            #Clean all temp variables
                            currValLabels = []
                            currValsDict = OrderedDict()
                            currentVar = ''
                        syntaxCommand = ''
                        line = line.replace(m.group(), '', 1)
            elif syntaxCommand == MISS_VALS or MISS_VALS_RE.match(line):
                if not syntaxCommand:
                    syntaxCommand = MISS_VALS
                    line = line.replace(MISS_VALS_RE.match(line).group(), '', 1)
                    
                #User defined missing values
                while line.strip():
                    #First validate if not ending a previous declaration
                    m = DECL_END_RE.match(line)
                    if m:
                        #Means that the end of the current variable is reached, but not the end of the command
                        
                        if currentVar:
                            #Save missing values in all the variables in the current range
                            setMissingValues(currentVar, currMissVals)
                            #Clean all temp variables
                            currMissVals = {}
                            currentVar = ''
                        line = line.replace(m.group(), '', 1) #Remove decl end /
                    
                    #Add or append new line to currentVar if a variable range
                    m = VARS_RANGE_RE.match(line)
                    if m:
                        currentVar += ' ' + m.group().strip()
                        line = line.replace(m.group(), '', 1) #Remove current matched var range from line
                        #Transform \n to \s
                        currentVar = getVar(currentVar.replace('\n', ' '))
                    
                    #If in missing values and current variable is enabled.
                    #get array or list of missing values...
                    m = VALS_ARR_RE.match(line)
                    if m:
                        line = line.replace(m.group(), '', 1).strip()
                        #Save missing values as array in the vars dict
                        currMissVals = getValueSet(m.group(1))
                    
                    m = COMMAND_END_RE.match(line)
                    if m:
                        if currentVar:
                            setMissingValues(currentVar, currMissVals)
                            #Clean all temp variables
                            currMissVals = {}
                            currentVar = ''
                        syntaxCommand = ''
                        line = line.replace(m.group(), '', 1)
            elif RECODE_CMD_RE.match(line):
                m = RECODE_CMD_RE.match(line)
                #Save recode info in variable...
                recodeOp = list('.' if val == 'sysmis' else val for val in m.group(2)[1:-1].split('='))
                setRecodeOp(m.group(1), recodeOp)
    print ('syntax file closed...')
    print ('----- syntax file read and metadata loaded in %s seconds -----' % round(time() - startTime, 2))
    
    BIG_TABLE_CSV = CSV_DIR + DATABASE + '.csv'
    TBL_LBL_CSV = CSV_DIR + DATABASE + '__lbl.csv'
    TBL_LBL_MV_CSV = CSV_DIR + DATABASE + '__lbl_mv.csv'

    
    #Measure csv creation execution time
    startTime = time()
    #Once all metadata is read write sql instructions and do posprocessing
    #Write all sql instructions for aux tables and big table
    bigTableCreateStmts = '\nCREATE TABLE ' + DATABASE + '( id integer, '
    bigTableAddKeysStmts = '\nALTER TABLE ' + DATABASE + ' ADD PRIMARY KEY (id), '

    #Write sql instructions for extra big tables...
    lblTblCreateStmts = '\nCREATE TABLE ' + DATABASE + '__lbl ( id integer, '
    lblTblAddKeyStmts = '\nALTER TABLE ' + DATABASE + '__lbl ADD PRIMARY KEY(id);\n\n\n'

    lblMissValsTblCreateStmts = '\nCREATE TABLE ' + DATABASE + '__lbl_mv ( id integer, '
    lblMissValsTblAddKeyStmts = '\nALTER TABLE ' + DATABASE + '__lbl_mv ADD PRIMARY KEY(id);\n\n\n'

    #Iterate over all variables to know the type of each column and if it is optionally a foreign key
    for var in varsDict:
        variable = varsDict[var]
        if VALUE_LABELS_KEY in variable:
            createAuxTableSQL(var, CSV_DIR + var + '.csv')
            #Save csv
            saveToCSV(CSV_DIR + var + '.csv', [NAME_KEY, LABEL_KEY], variable[VALUE_LABELS_KEY])
        columnName = var if var not in PSQL_R_W else '"' + var + '"'
        #Add column to creation statements (depends of var type)
        bigTableCreateStmts += columnName + ' ' + variable[TYPE_KEY] + ', '
        #(var type or text if foreign key)
        lblTblCreateStmts += columnName + ' '
        #Always text
        lblMissValsTblCreateStmts += columnName + ' text, '
        #Validate if var is a foreign key column
        if isForeignKey(var):
            bigTableAddKeysStmts += 'ADD FOREIGN KEY (' + columnName + ') REFERENCES ' + columnName + '(id), '
            lblTblCreateStmts += 'text, '
        #If not a foreign key then the column type must be the same as the declared in the variable
        #Except there is declared labels that are not missing vals...
        elif VAL_DICT_KEY in variable and MISSING_VALUES_KEY in variable and not set(variable[VAL_DICT_KEY].keys()).issubset(variable[MISSING_VALUES_KEY]):
            #Means that if not all declared value labels are missing values then use text type instead because of possible use of labels
            lblTblCreateStmts += 'text, '
        else:
            lblTblCreateStmts += variable[TYPE_KEY] + ', '

    bigTableCreateStmts = bigTableCreateStmts[:-2] + ' );\n\n\n'
    bigTableAddKeysStmts = bigTableAddKeysStmts[:-2] + ' ;\n\n\n'
    lblTblCreateStmts = lblTblCreateStmts[:-2] + ' );\n\n\n'
    lblMissValsTblCreateStmts = lblMissValsTblCreateStmts[:-2] + ' );\n\n\n'


    tableCommentStmts += "\nCOMMENT ON TABLE " + DATABASE + " IS 'QUESTIONARIE DATA FACT TABLE';\n\n\n" + \
    "COMMENT ON TABLE " + DATABASE + "__lbl IS 'QUESTIONARIE DATA FACT TABLE WITH LABELS AND MISS VALS AS NULL';\n\n\n" + \
    "COMMENT ON TABLE " + DATABASE + "__lbl_mv IS 'QUESTIONARIE DATA FACT TABLE WITH LABELS OF MISS AND NON MISS VALS';\n\n\n"
    copyDataStmts += '\n\COPY ' + DATABASE + " FROM '" + BIG_TABLE_CSV + "' WITH DELIMITER ',' CSV HEADER;\n\n\n" + \
    '\COPY ' + DATABASE + "__lbl FROM '" + TBL_LBL_CSV + "' WITH DELIMITER ',' CSV HEADER;\n\n\n" + \
    '\COPY ' + DATABASE + "__lbl_mv FROM '" + TBL_LBL_MV_CSV + "' WITH DELIMITER ',' CSV HEADER;\n\n\n"

    with open(SQL_FILE, 'a', encoding = ENCODING) as sqlFile:
        sqlFile.write(createStmts + bigTableCreateStmts + lblTblCreateStmts + lblMissValsTblCreateStmts + 
        tableCommentStmts + copyDataStmts + addKeysStmts + bigTableAddKeysStmts +
        lblTblAddKeyStmts + lblMissValsTblAddKeyStmts + 'ANALYZE;\n\n\nCOMMIT;\n\n\n\n\n')
        
    print ('aux tables csv created and sql instructions written...')
    print ('----- time of execution: %s seconds -----' % round(time() - startTime, 2))
    
    dataFilename = INPUT_DIR + dataFilename
    #Start separating the fields in big data file with commas
    print ('Reading data file [' + dataFilename + ']...')
    startTime = time()
    with open(dataFilename, encoding = ENCODING) as dataFile:
        bigTableCSV = open(BIG_TABLE_CSV, 'w', encoding = ENCODING, newline = '')
        tblLblCSV = open(TBL_LBL_CSV, 'w', encoding = ENCODING, newline = '')
        tblLblMVCSV = open(TBL_LBL_MV_CSV, 'w', encoding = ENCODING, newline = '')
        
        #Write header
        bigTableCSV.write('id,' + ','.join([var for var in varsDict]) + '\n')
        tblLblCSV.write('id,' + ','.join([var for var in varsDict]) + '\n')
        tblLblMVCSV.write('id,' + ','.join([var for var in varsDict]) + '\n')
        #Make csv writers
        bigTableWriter = csv.writer(bigTableCSV, lineterminator = '\n')
        tblLblWriter = csv.writer(tblLblCSV, lineterminator = '\n')
        tblLblMVWriter = csv.writer(tblLblMVCSV, lineterminator = '\n')
        
        #Iterate over all lines of the data file
        pk = 1
        for line in dataFile:
            #Iterate each line for the var positions... n^2
            #Hearbeat every 10,000 lines...
            if not pk % 10000:
                print('.', end = '', flush = True)

            #Don't forget id!! Must be manually added, not serial (for more control)
            newLine = [str(pk)]
            lblLine = [str(pk)]
            lblMVLine = [str(pk)]
            for varName in varsDict:
                var = varsDict[varName]
                varVal = line[var[START_KEY] - 1:var[END_KEY]].strip()
                #Do recoding of values in variables...
                #Considering simplest recode case, a value for a value...
                if RECODE_KEY in var and varVal == var[RECODE_KEY][0]:
                    varVal = var[RECODE_KEY][1]
                
                #If no value...
                varVal = None if varVal == '.' or varVal == '' else varVal
                
                #If it is a foreign key and the id doesnt exists create it (must be not null)
                if varVal and isForeignKey(varName) and varVal not in var[VAL_DICT_KEY]:
                    #Just validate if a number and numerically equal then abort
                    if not ((var[TYPE_KEY] == NUMERIC_TYPE or var[TYPE_KEY] == DOUBLE_TYPE or var[TYPE_KEY] == INT_TYPE) and float(varVal) in list(float(x) for x in var[VAL_DICT_KEY])):
                        valLabel = 'Undefined value label'
                        print ('No key ' + varVal + ' for variable: ' + varName + '! adding it...')
                        with open(CSV_DIR + varName + '.csv', 'a', encoding = ENCODING) as f:
                            f.write(varVal + ',' + valLabel + '\n')
                        
                        var[VAL_DICT_KEY][varVal] = valLabel
                        var[VALUE_LABELS_KEY].append({NAME_KEY: varVal, LABEL_KEY: valLabel})

                lblVar = varVal
                lblMVVar = varVal
                if VAL_DICT_KEY in var and varVal in var[VAL_DICT_KEY]:
                    lblMVVar = lblVar = var[VAL_DICT_KEY][varVal]
                if MISSING_VALUES_KEY in var and varVal in var[MISSING_VALUES_KEY]:
                    lblVar = None
                
                newLine.append(varVal)
                lblLine.append(lblVar)
                lblMVLine.append(lblMVVar)
                
            #Write line to files...
            bigTableWriter.writerow(newLine)
            tblLblWriter.writerow(lblLine)
            tblLblMVWriter.writerow(lblMVLine)
            pk = pk + 1
        #Close big tables csv's before closing original data file
        bigTableCSV.close()
        tblLblCSV.close()
        tblLblMVCSV.close()
    print ('data file closed...')
    print ('----- big table and extra tables csv written in %s seconds -----' % round(time() - startTime, 2))

    print (' === Total execution time of ' + DATABASE + ' dataset: %s seconds === ' % round(time() - dsStartTime, 2))
    print ('Dumped dataset ' + DATABASE + ' with ' + str(len(varsDict)) + ' variables and ' + str(pk) + ' cases')
    print (' <<< END OF DATASET >>>')
print ('---- <<< END OF CSV DUMP EXECUTION >>> ----')
print (' <<=== Total csv dump execution time: %s seconds ===>> ' % round(time() - dumpStartTime, 2))