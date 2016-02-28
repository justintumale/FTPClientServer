

size = 5000000
s = ""
fn = "bigtext3.txt"
for i in range(size):
    s += str(i) + '\n'

with open(fn, 'w') as f:
    f.write(s)
print f.closed
