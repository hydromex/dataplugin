import requests
from bs4 import BeautifulSoup
import sys
import datetime
import os
import ujson

def isfloat(value):
  try:
    x = float(value)
    return x
  except ValueError:
    return False

#Must be the same as the descriptor file
PLUGIN_NAME = 'QueretaroSummaryPlugin'
PLUGIN_VERSION = '1.0.0'

login_url = 'https://app1.fluxsuite.com/login'
download_url = 'https://app1.fluxsuite.com/stations/26/reports/summary'
firstDate = '2016-08-18'
lastDate = datetime.datetime.today().strftime('%Y-%m-%d')

db_meta_url = 'http://localhost:8080/dataplugin/rs/meta/register-db'

#startDate=2017-06-01&endDate=2017-06-02'
if len(sys.argv) > 3:
    endDate = sys.argv[3]
    startDate = sys.argv[2]
elif len(sys.argv) > 2:
    startDate = sys.argv[2]
    endDate = lastDate
else:
    startDate = firstDate
    endDate = lastDate

outputDirectory = sys.argv[1]
if os.path.isdir(outputDirectory):
    download_url += '?startDate=' + startDate + '&endDate=' + endDate
    email = 'galopez@uabc.edu.mx'
    password = 'uaq2015'

    login_res = requests.get(login_url, verify=False)
    soup = BeautifulSoup(login_res.text, 'html.parser')
    csrf = soup.find('input',{'id':'csrf_token'})['value']
    cookies = login_res.cookies

    data = {'csrf_token':csrf, 'email':email, 'password':password, 'submit': 'Login',
            'next': download_url}
    try:
        res = requests.post(login_url, verify=False, data=data, cookies=cookies, stream=True)
    except requests.exceptions.RequestException as e:
        print e
        sys.exit(1)
    if res.status_code == 200:
        filename = 'data.json'
        # unitfilename = 'units.json'
        fileWithDir = os.path.join(outputDirectory, filename)
        # unitFileWithDir = os.path.join(outputDirectory, unitfilename)
        with open(fileWithDir, 'w') as jsonfile:
            headers = []
            units = []
            values = []
            # sizes = []
            # types = []
            for chunk in res.iter_content(chunk_size=128):
                if len(values) <= 204:
                    for line in chunk.split("\n"):
                        linevalues = line.split("\t")
                        if linevalues[0] and values:
                            values[-1] += linevalues[0]
                            del linevalues[0]
                        if not linevalues[0]:
                            del linevalues[0]

                        values.extend(linevalues)
                        if len(values) == 204:
                            if not headers:
                                headers = values
                            elif not units:
                                units = list(map((lambda x: x[1:-1] if x not in ['DATAU', '186'] else x), values))
                                # json = dict(zip(headers, units))
                                # with  open(unitFileWithDir, 'w') as unitfile:
                                #     ujson.dump(json, unitfile)
                                #     unitfile.write('\n')
                            else:
                                for i, v in enumerate(values):
                                    x = isfloat(v)
                                    if x:
                                        if v == 'NaN':
                                            values[i] = None
                                        else:
                                            values[i] = x
                                        # v = x
                                    elif v.isdigit():
                                        x = int(v)
                                        values[i] = x
                                        # v = x

                                    # s = sys.getsizeof(v)
                                    # if len(sizes) < 204:
                                    #     sizes.append(s)
                                    #     t = type(v)
                                    #     if t is int:
                                    #         t = 'integer'
                                    #     elif t is float:
                                    #         t = 'float'
                                    #     else:
                                    #         t = 'string'
                                    #     types.append(t)
                                    # elif s > sizes[i]:
                                    #     sizes[i] = s
                                json = dict(zip(headers, values))
                                ujson.dump(json, jsonfile)
                                jsonfile.write('\n')
                            values = []
            # json = [{"table":"data", 'variables': []},
            #     {"table":"units", "variables":[]}]
            # for i, h in enumerate(headers):
            #     var = {"name": h, 'size': sizes[i], 'type': types[i],
            #     'description': "variable: " + h + " in unit " + units[i]}
            #     json[0]['variables'].append(var)
            #     var = {"name": h, 'size': sys.getsizeof(units[i]), 'type': 'string',
            #         'description': "Units used by variable: " + h}
            #     json[1]['variables'].append(var)
            # ujson.dump(json, jsonfile)

            #Insert/Update database metadata
            requests.post(db_meta_url,
            data = {'db-name': 'queretarosummary', 'plugin-name': PLUGIN_NAME,
            'plugin-version': PLUGIN_VERSION, 'db-schema': '{}'})
        print("File created in: " + fileWithDir)
else:
    print("Error: Directory doesn't exist")
