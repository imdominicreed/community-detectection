import networkx as nx
import os

for file in os.listdir('graph'):
    graph = "graph/" + file
    G = nx.read_edgelist(graph, nodetype=int)
    Gcc = sorted(nx.connected_components(G), key=len, reverse=True)
    G0 = G.subgraph(Gcc[0])
    nx.write_edgelist(G0, "subgraph/" + file + "sub_graph", data=False)
