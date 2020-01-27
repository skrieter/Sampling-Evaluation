import sys
import re

file_path = sys.argv[1]
file_name = sys.argv[2]

print(file_path)
print(file_name)

with open(file_path + file_name, "r") as f:
	lines = f.readlines()
	with open(file_path + "_" + file_name, "w") as f:
		for line in lines:
		    line = re.sub(r"^(c\s+\d+\s+\w+)\s+.*\n$", r"\1\n", line)
		    f.write(line)
		    #if not re.match(r"^(c\s+\d+\s+\w+)\s+[^bool]\s*.*$", line):
			#    line = re.sub(r"^(c\s+\d+\s+\w+)\s+bool\s*$", r"\1\n", line)
			#    f.write(line)