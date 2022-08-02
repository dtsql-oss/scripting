import glob
import matplotlib.pyplot as plt
import numpy as np
from numpy import genfromtxt

evaluation_files = glob.glob("*_evaluation_*.csv")
choice_string =  "\n".join(["[" + str(i) + "]: " + item for i, item in enumerate(evaluation_files)])

print(choice_string)
choice = int(input("choice: "))

my_data = genfromtxt(evaluation_files[choice], delimiter=',')
_, ay = np.split(my_data, 2, axis=1)
ayy = np.squeeze(np.asarray(ay))

x = np.arange(len(ay))

ax = plt.gca()
ax.set_ylim([0, 900])
y = ayy
plt.plot(x, y)
plt.xlabel('x')
plt.ylabel('y')
plt.show()
