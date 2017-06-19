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

login_url = 'https://app1.fluxsuite.com/login'
download_url = 'https://app1.fluxsuite.com/stations/26/reports/summary'
firstDate = '2016-08-18'
lastDate = datetime.datetime.today().strftime('%Y-%m-%d')

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
        filename = 'summary_report_' + startDate + '_' + endDate + '.json'
        fileWithDir = os.path.join(outputDirectory, filename)
        with open(fileWithDir, 'w') as jsonfile:
            headers = []
            units = []
            keys = ['value', 'unit']
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
                                groups = zip(values, units)
                                json = [dict(zip(keys,group)) for group in groups]
                                json = dict(zip(headers, json))
                                ujson.dump(json, jsonfile)
                                jsonfile.write('\n')
                            values = []
            # json = {"table":"data", 'variables': []}
            # for i, h in enumerate(headers):
            #     var = {"name": h, 'size': sizes[i], 'type': types[i],
            #     'description': "variable: " + h + " in unit " + units[i]}
            #     json['variables'].append(var)
            # ujson.dump(json, jsonfile)
        print("File created in: " + fileWithDir)
else:
    print("Error: Directory doesn't exist")
