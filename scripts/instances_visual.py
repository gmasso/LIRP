#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Wed May 23 10:19:22 2018

@author: gmas
"""

import numpy as np
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D
import json
import math
from scipy.stats import norm
import random


inst_filename = "../Instances/Complete/Small/2l3dc0-25r-1c-30p-GauL_5c736014-4296-4cc1-9685-7290e3709e92.json"

random.seed()
with open(inst_filename) as inst_file:
    inst_json = json.load(inst_file)
        
    dc_json = inst_json['depots layers'][0]
    sites_json = dc_json['sites']
    coords_dc = np.zeros((len(sites_json), 2))
    id = 0
    for loc_json in sites_json:
        coords_dc[id] = np.array(loc_json['coordinates'])
        id += 1
    
    grid_size = dc_json['map size']

    cities_json = inst_json['clients']['cities']['sites']
    sizes_json = inst_json['clients']['cities']['sizes']
    coords_cities = np.zeros((len(cities_json), 2))
    cities_sizes = np.zeros(len(sizes_json))
    id = 0
    for size in sizes_json:
        cities_sizes[id] = size
        id += 1
        
    id = 0
    for loc_json in cities_json:
        coords_cities[id] = np.array(loc_json['coordinates'])
        id += 1
        
    clients_json = inst_json['clients']['sites']
    coords_clients = np.zeros((len(clients_json), 2))
    id = 0
    for loc_json in clients_json:
        coords_clients[id] = np.array(loc_json['coordinates'])
        id += 1
        
    cities_influence = 0
    for city_id in range(len(coords_cities)):
        cities_influence += cities_sizes[city_id]

    demand_grid = np.zeros((grid_size, grid_size))
    for x in range(grid_size):
        for y in range(grid_size):
            intensity = 0
            city_size = 0
            dist_with_city = 0
            for city_id in range(len(coords_cities)):
                dist_with_city = math.sqrt((coords_cities[city_id, 0] - (x + 0.5))**2 + (coords_cities[city_id, 1] - (y + 0.5))**2)
                city_size = cities_sizes[city_id]
                intensity += city_size * (norm(0, city_size).cdf(5000.0 / (dist_with_city**2)))
            if cities_influence > 0:
                demand_grid[x, y] = 0.25 * (0.75 + 0.5 * random.random()) + 0.75 * intensity / cities_influence * (0.75 + 0.5 * random.random())
            else:
                demand_grid[x, y] = random.random()

    x = np.arange(grid_size)
    y = np.arange(grid_size)
    plt.contourf(x, y, demand_grid, cmap='jet')
    #colorbar(surf, shrink=0.5, aspect=5)
    plt.show()
    
#    print(demand_grid)
#    print(coords_dc)
#    print(coords_cities)
#    print(coords_clients) 