#! /usr/bin/python

import requests

import sys
import os
import zipfile
import time
import json
from bson import json_util
import ujson
from datetime import datetime
from os import path
from math import isinf

PLUGIN_NAME = 'QueretaroPlugin'
PLUGIN_VERSION =  '2.0.0'
DB = 'ghcn'

db_meta_url = 'http://localhost:8080/dataplugin/rs/meta/register-db'

def extractghg(filename):
	starttime = time.time()
	result = {}
	ghg = zipfile.ZipFile(filename, 'r')
	try:
		files = ghg.namelist()
		for file in files:
			if file.endswith('.data'):
				if 'biomet' in file:
					result['biometdata'] = ghg.read(file)
				else:
					result['data'] = ghg.read(file)
			elif file.endswith('.metadata'):
				if 'biomet' in file:
					result['biometmetadata'] = ghg.read(file)
				else:
					result['metadata'] = ghg.read(file)
	except Exception as e:
		print e
	else:
		ghg.close()
		elapsedtime = time.time() - starttime
		print "Extract ghg time: " + str(elapsedtime)
		return result

def processdata(filename, filedate, filedata, outputDir):
	starttime = time.time()
	if not filedata:
		return
	lines = filedata.splitlines()
	header = {'filename':filename, 'filedate':filedate}
	variables = []
	for line in lines[0:6]:
		[variable, value] = line.split(':\t')
		value = tofloatorint(value)
		header[variable.replace(' ', '_')] = value

	variables = lines[7].split('\t')

	auxtime = time.time()
	for line in lines[8:]:
		data = header
		values = line.split('\t')
		ints = [int(x) for x in values[1:6]]
		floats = [float(x) for x in values[8:]]
		if floats[17] == float('inf'):
			floats[17] = "Infinity"
		elif floats[17] == float('-inf'):
			floats[17] = "-Infinity"
		newValues = [values[0]]
		newValues.extend(ints)
		newValues.extend(values[6:8])
		newValues.extend(floats)
		data.update(dict(zip(variables, newValues)))
		with open(outputDir + 'data.json', 'a') as jsonfile:
			ujson.dump(data, jsonfile)
			jsonfile.write('\n')
	print "Writing in json: " + str(time.time() - auxtime)

	elapsedtime = time.time() - starttime
	print "Process data time: " + str(elapsedtime)

def processmetadata(filename, filedate, metadata, outputDir):
	starttime = time.time()
	data = {'filename':filename, 'date':filedate}
	data['instruments'] = []
	data['columns'] = []
	if not metadata:
		return
	lines = metadata.splitlines()
	instrument = {}
	column = {}
	section = ''
	for line in lines:
		if line in ['', ';GHG_METADATA']:
			if section == 'Instruments':
				data['instruments'].append(instrument)
				instrument = {}
			elif section == 'FileDescription':
				if column != {}:
					data['columns'].append(column)
					column = {}
		elif line[0] == '[':
			section = line.partition('[')[-1].rpartition(']')[0]
			if section != 'Instruments' and section != 'FileDescription':
				section = 'Metadata'
		else:
			[variable, value] = line.split('=')
			if not value:
				continue
			value = tofloatorint(value)
			if section == 'Instruments':
				variable = variable.split('_', 2)[2]
				instrument[variable] = value
			elif section == 'FileDescription':
				if variable.startswith('col'):
					variable = variable.split('_')[2]
					column[variable] = value
				else:
					data[variable] = value
			else:
				data[variable] = value
	with open(outputDir + 'metadata.json', 'a') as jsonfile:
		ujson.dump(data, jsonfile)
		jsonfile.write('\n')
	elapsedtime = time.time() - starttime
	print "Process metadata time: " + str(elapsedtime)

def processresult(data, outputDir):
	starttime = time.time()
	lines = data.splitlines()
	result = {}
	newSection = {}
	sections = lines[0].split(',')
	columns = lines[1].split(',')
	formats = lines[2].split(',')
	values = lines[3].split(',')
	size = len(columns)

	result['filename'] = values[0]
	filedate = datetime.strptime(os.path.basename(filename)[:17], '%Y-%m-%dT%H%M%S')
	result['filedate'] = filedate

	currentSection = sections[0]
	result[currentSection] = []
	newValue = {}

	for i in range(2, size):
		if sections[i]:
			currentSection = sections[i]

		result[columns[i]] = {}
		result[columns[i]]['value'] = tofloatorint(values[i])
		result[columns[i]]['format'] = formats[i]
		result[columns[i]]['section'] = currentSection

	with open(outputDir + 'result.json', 'a') as jsonfile:
		json.dump(result, jsonfile, default=json_util.default)
		jsonfile.write('\n')
	elapsedtime = time.time() - starttime
	print "Process result time: " + str(elapsedtime)


def tofloatorint(value):
	if isint(value):
		return int(value)
	elif isfloat(value):
		return float(value)
	else:
		return value

def isfloat(value):
  try:
    float(value)
    return True
  except:
    return False

def isint(value):
  try:
    int(value)
    return True
  except:
    return False

totaltime = time.time()

if len(sys.argv) > 1:
	rawdir = sys.argv[1]
if len(sys.argv) > 2:
	resultdir = sys.argv[2]
if len(sys.argv) > 3:
	#Format dd/mm/yyyy
	fromdate = datetime.strptime(sys.argv[3], "%d/%m/%Y")
if len(sys.argv) > 4:
	todate = datetime.strptime(sys.argv[4], "%d/%m/%Y")

outputDir = os.path.abspath(os.path.join(sys.argv[0], os.pardir)) + os.sep

if rawdir:
	rawfiles = os.listdir(rawdir)
	with open(outputDir + 'metadata.json', 'w'):
		pass
	with open(outputDir + 'data.json', 'w'):
		pass
if resultdir:
	resultfiles = os.listdir(resultdir)
	with open(outputDir + 'result.json', 'w'):
		pass

# Process all raw files (metadata and data files)
for filename in rawfiles:
	starttime = time.time()

	# If there is a date as an argument, the script only
	# processes the files equal to or after the date
	filedate = datetime.strptime(os.path.basename(filename)[:17], '%Y-%m-%dT%H%M%S')
	if len(sys.argv) > 3:
		if filedate <= fromdate:
			continue

	if len(sys.argv) > 4:
		if filedate >= todate:
			continue

	# Process all GHG files
	if(filename.endswith('.ghg')):
		# Extract the file sinside the GHG file
		files = extractghg(rawdir + '/' + filename)
		if 'metadata' in files:
			#Process and save metadata in json file
			processmetadata(filename, filedate, files['metadata'], outputDir)
		if 'data' in files:
			#Process and save data in json file
			processdata(filename, filedate, files['data'], outputDir)

	elapsedtime = time.time() - starttime
	print "File " + filename + " time: " + str(elapsedtime)

# Process al result files
for filename in resultfiles:
	starttime = time.time()

	# If there is a date as an argument, the script only
	# processes the files equal to or after the date
	filedate = datetime.strptime(os.path.basename(filename)[:17], '%Y-%m-%dT%H%M%S')
	if len(sys.argv) > 3:
		if filedate <= fromdate:
			continue

	if len(sys.argv) > 4:
		if filedate >= todate:
			continue

	if filename.endswith('.zip'):
		resultzip = zipfile.ZipFile(resultdir + '/' + filename, 'r')
		filelist = resultzip.namelist()
		for file in filelist:
			if file.endswith('.csv'):
				processresult(resultzip.read(file), outputDir)

	elapsedtime = time.time() - starttime
	print "File " + filename + " time: " + str(elapsedtime)

#Insert/Update database metadata
requests.post(db_meta_url, data = {'db-name': DB, 'plugin-name': PLUGIN_NAME,
                                   'plugin-version': PLUGIN_VERSION, 'db-schema': '{}'})

elapsedtime = time.time() - totaltime
print "Total time: " + str(elapsedtime)