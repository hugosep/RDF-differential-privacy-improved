import random

from os import listdir
from os.path import isfile, join
onlyfiles = [f for f in listdir("working") if isfile(join("working", f))]
onlyfiles = onlyfiles * 10

planCache = open("planCache.txt", "w")

random.shuffle(onlyfiles)

for file in onlyfiles:
    planCache.write("resources/wikidata_queries/working/" + file + "\n")

planCache.close()
