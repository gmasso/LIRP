#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Tue May 22 11:20:54 2018

@author: gmas
"""
from os import listdir
from os.path import isdir
import json

sol_dir = "../Solutions/"
list_sol = [d for d in listdir(sol_dir)]

nbSol = 0
imp10 = 0
imp25_3 = 0
imp25_6 = 0

for d in listdir(sol_dir):
    if(isdir(sol_dir + d) and ('10r' in d or '25r' in d)):
        with open(sol_dir + d + "/LM-Split[0,100]_sol.json") as lm_file:
            lm_json = json.load(lm_file)
        with open(sol_dir + d + "/Split[0,100]_sol.json") as split_file:
            split_json = json.load(split_file)
        with open(sol_dir + d + "/NoSplit_sol.json") as noSplit_file:
            noSplit_json = json.load(noSplit_file)
        
        noSplit_gap = (noSplit_json['objective value']['total']- noSplit_json['LB']) / noSplit_json['LB']
        split_gap = (split_json['objective value']['total']- noSplit_json['LB']) / noSplit_json['LB']
        lm_gap = (lm_json['objective value']['total']- noSplit_json['LB']) / noSplit_json['LB']
        if(lm_gap <= noSplit_gap or split_gap <= noSplit_gap):
            if('10r' in d):
                print("Improved Gap (10 retailers)")
                imp10 += 1
            else: 
                print("Improved Gap (25 retailers)")
                if('3dc0' in d):
                    imp25_3 += 1
                else:
                    imp25_6 += 1
                            
            print("gap with solver: ", noSplit_gap, ", sol time: ", noSplit_json['resolution time'])
            print("gap with split: ", split_gap, ", sol time: ", split_json['resolution time'])
            print("gap with lm: ", lm_gap, ", sol time: ", lm_json['resolution time'])

        nbSol += 1
    
print("Nb solutions calculated : ", nbSol)
print("Improved the gap for ", imp10, " instances with 10 retailers, ", imp25_3, " instances with 25 retailers (3 dc) and ", imp25_6, " instances with 25 retailers (6 dc)")
