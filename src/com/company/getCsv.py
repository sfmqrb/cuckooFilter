import re
from typing import Dict

header = [
    "numofSamples",
    "bucketAddressSpace",
    "bucketSize",
    "maxNumOfKicks",
    "maxSampleByteLen",
    "fingerprintLen",
    "insertTime",
    "lookupTime",
    "deleteTime",
    "InsertFailedAt",
    "LookupFailedAt",
    "DeleteFailedAt",
]

bigd = {}
with open('res.csv', 'r') as f:
    sls = f.readlines()
    for i, s in enumerate(sls):
        ls = list(map(lambda x: int(x), s[1:-2].split(',')))
        d = {header[i] : ls[i] for i in range(len(ls))}
        bigd[i] = d

print('bigd:', bigd)   



import json
import csv
 

data_file = open('res2.csv', 'w', newline='')
csv_writer = csv.writer(data_file)
 
count = 0
for i in bigd:
    data = bigd[i]
    if count == 0:
        header = data.keys()
        csv_writer.writerow(header)
        count += 1
    csv_writer.writerow(data.values())
 
data_file.close()