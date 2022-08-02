import datetime
import matplotlib.pyplot as plt
import numpy as np
import shutil
from numpy import genfromtxt

my_data = genfromtxt("evaluation_file_1.csv", delimiter=';')
_, ay = np.split(my_data, 2, axis=1)
ayy = np.squeeze(np.asarray(ay))

x = np.arange(743)
noise = np.around(np.random.normal(775, 4, 743), decimals=2)

ax = plt.gca()
ax.set_ylim([0, 900])
y = ayy + noise
plt.plot(x, y)
plt.xlabel('x')
plt.ylabel('y')
plt.show()

shutil.copyfile("evaluation_file_1.csv", "evaluation_file_1_noise.csv")

# opening the file in read mode
file = open("evaluation_file_1_noise.csv", "r")
replacement = "Date,Value [kW]\n"
# using the for loop
i = 0
first_date = None
for line in file:
    line = line.strip()
    date = datetime.datetime.strptime(line.split(";")[0], "%Y-%m-%d %H:%M:%S.%f") if len(line) > 0 else ""
    if first_date is None and date != "":
        date = date - datetime.timedelta(days=5 * 365 + 1)
        first_date = date
    if date != "":
        date = first_date + datetime.timedelta(days=i)
        date = date.replace(tzinfo=datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    changes = str(date) + (("," + str(y[i])) if (i < len(y)) else "")
    i += 1
    if len(changes.strip()) > 0:
        replacement = replacement + changes + "\n"

file.close()
# opening the file in write mode
fout = open("evaluation_file_1_noise.csv", "w")
fout.write(replacement)
fout.close()
