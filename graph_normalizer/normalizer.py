import os


for file in os.listdir('graph'):
    n = 0
    if '.DS_Store' == file:
        continue
    write = open("normalized/" + file, "w")
    f = open("graph/" + file, "r")
    m = 0
    d = {}
    for line in f.readlines():
        src, dst = map(int, line.split())
        m = max(src, dst, m)
        if src not in d:
            d[src] = n
            n += 1
        if dst not in d:
            d[dst] = n
            n += 1
        write.write(str(d[src]) + " " + str(d[dst]) + '\n')
    print("graph/" + file, m, n)
