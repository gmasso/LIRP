#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Tue May 22 11:20:54 2018

@author: gmas
"""
from os import listdir
from os.path import isdir
import json
import pandas as pd
import math

sol_dir = "../Solutions/"
list_sol = [d for d in listdir(sol_dir)]

nbSol = 0
imp10 = 0
imp25_3 = 0
imp25_6 = 0

results_3dc10r = pd.DataFrame(0, index=[7, 14, 30], columns=pd.MultiIndex.from_product([['L', 'M', 'H'], ['Cplex', 'RS', 'LM + RS'], ['Avg Gap', 'Max Gap', '% Opt', 'BetterOrEqual']]))
results_3dc25r = pd.DataFrame(0, index=[7, 14, 30], columns=pd.MultiIndex.from_product([['L', 'M', 'H'], ['Cplex', 'RS', 'LM + RS'], ['Avg Gap', 'Max Gap', '% Opt', 'BetterOrEqual']]))
results_6dc25r = pd.DataFrame(0, index=[7, 14, 30], columns=pd.MultiIndex.from_product([['L', 'M', 'H'], ['Cplex', 'RS', 'LM + RS'], ['Avg Gap', 'Max Gap', '% Opt', 'BetterOrEqual']]))

gaps = [];

for d in listdir(sol_dir):
    demand_intens = 'H'
    if('L_' in d):
        demand_intens = 'L'
    else:
        if('M_' in d):
            demand_intens = 'M'

    nb_periods = 30
    if('7p-' in d):
        nb_periods = 7
    else:
        if('14p-' in d):
            nb_periods = 14
    
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

        if('10r' in d):
            if(lm_gap < math.pow(10, -4)):
                results_3dc10r.loc[nb_periods, (demand_intens, 'LM + RS', '% Opt')] += 1 
            else:
                results_3dc10r.loc[nb_periods, (demand_intens, 'LM + RS', 'Avg Gap')] += lm_gap
                if(lm_gap > results_3dc10r.loc[nb_periods, (demand_intens, 'LM + RS', 'Max Gap')]):
                    results_3dc10r.loc[nb_periods, (demand_intens, 'LM + RS', 'Max Gap')] = lm_gap
            if(lm_gap <= noSplit_gap):
                results_3dc10r.loc[nb_periods, (demand_intens, 'LM + RS', 'BetterOrEqual')] += 1
                
            if(split_gap < math.pow(10, -4)):
                results_3dc10r.loc[nb_periods, (demand_intens, 'RS', '% Opt')] += 1 
            else:
                results_3dc10r.loc[nb_periods, (demand_intens, 'RS', 'Avg Gap')] += split_gap
                if(split_gap > results_3dc10r.loc[nb_periods, (demand_intens, 'RS', 'Max Gap')]):
                    results_3dc10r.loc[nb_periods, (demand_intens, 'RS', 'Max Gap')] = split_gap
            if(split_gap <= noSplit_gap):
                results_3dc10r.loc[nb_periods, (demand_intens, 'RS', 'BetterOrEqual')] += 1
                
            if(noSplit_gap < math.pow(10, -4)):
                results_3dc10r.loc[nb_periods, (demand_intens, 'Cplex', '% Opt')] += 1 
            else:
                results_3dc10r.loc[nb_periods, (demand_intens, 'Cplex', 'Avg Gap')] += noSplit_gap
                if(noSplit_gap > results_3dc10r.loc[nb_periods, (demand_intens, 'Cplex', 'Max Gap')]):
                    results_3dc10r.loc[nb_periods, (demand_intens, 'Cplex', 'Max Gap')] = noSplit_gap
            if(noSplit_gap <= noSplit_gap):
                results_3dc10r.loc[nb_periods, (demand_intens, 'Cplex', 'BetterOrEqual')] += 1
                
        if('3dc0' in d and '25r' in d):
            if(lm_gap < math.pow(10, -4)):
                results_3dc25r.loc[nb_periods, (demand_intens, 'LM + RS', '% Opt')] += 1 
            else:
                results_3dc25r.loc[nb_periods, (demand_intens, 'LM + RS', 'Avg Gap')] += lm_gap
                if(lm_gap > results_3dc25r.loc[nb_periods, (demand_intens, 'LM + RS', 'Max Gap')]):
                    results_3dc25r.loc[nb_periods, (demand_intens, 'LM + RS', 'Max Gap')] = lm_gap
            if(lm_gap <= noSplit_gap):
                results_3dc25r.loc[nb_periods, (demand_intens, 'LM + RS', 'BetterOrEqual')] += 1
                
            if(split_gap < math.pow(10, -4)):
                results_3dc25r.loc[nb_periods, (demand_intens, 'RS', '% Opt')] += 1
            else:
                results_3dc25r.loc[nb_periods, (demand_intens, 'RS', 'Avg Gap')] += split_gap
                if(split_gap > results_3dc25r.loc[nb_periods, (demand_intens, 'RS', 'Max Gap')]):
                    results_3dc25r.loc[nb_periods, (demand_intens, 'RS', 'Max Gap')] = split_gap
            if(split_gap <= noSplit_gap):
                results_3dc25r.loc[nb_periods, (demand_intens, 'RS', 'BetterOrEqual')] += 1
                
            if(noSplit_gap < math.pow(10, -4)):
                results_3dc25r.loc[nb_periods, (demand_intens, 'Cplex', '% Opt')] += 1 
            else:
                results_3dc25r.loc[nb_periods, (demand_intens, 'Cplex', 'Avg Gap')] += noSplit_gap
                if(noSplit_gap > results_3dc25r.loc[nb_periods, (demand_intens, 'Cplex', 'Max Gap')]):
                    results_3dc25r.loc[nb_periods, (demand_intens, 'Cplex', 'Max Gap')] = noSplit_gap
            if(noSplit_gap <= noSplit_gap):
                results_3dc25r.loc[nb_periods, (demand_intens, 'Cplex', 'BetterOrEqual')] += 1

        if('6dc0' in d and '25r' in d):
            if(lm_gap < math.pow(10, -4)):
                results_6dc25r.loc[nb_periods, (demand_intens, 'LM + RS', '% Opt')] += 1 
            else:
                results_6dc25r.loc[nb_periods, (demand_intens, 'LM + RS', 'Avg Gap')] += lm_gap
                if(lm_gap > results_6dc25r.loc[nb_periods, (demand_intens, 'LM + RS', 'Max Gap')]):
                    results_6dc25r.loc[nb_periods, (demand_intens, 'LM + RS', 'Max Gap')] = lm_gap
            if(lm_gap <= noSplit_gap):
                results_6dc25r.loc[nb_periods, (demand_intens, 'LM + RS', 'BetterOrEqual')] += 1
                
            if(split_gap < math.pow(10, -4)):
                results_6dc25r.loc[nb_periods, (demand_intens, 'RS', '% Opt')] += 1
            else:
                results_6dc25r.loc[nb_periods, (demand_intens, 'RS', 'Avg Gap')] += split_gap                
                if(split_gap > results_6dc25r.loc[nb_periods, (demand_intens, 'RS', 'Max Gap')]):
                    results_6dc25r.loc[nb_periods, (demand_intens, 'RS', 'Max Gap')] = split_gap
            if(split_gap <= noSplit_gap):
                results_6dc25r.loc[nb_periods, (demand_intens, 'RS', 'BetterOrEqual')] += 1
                
            if(noSplit_gap < math.pow(10, -4)):
                results_6dc25r.loc[nb_periods, (demand_intens, 'Cplex', '% Opt')] += 1 
            else:
                results_6dc25r.loc[nb_periods, (demand_intens, 'Cplex', 'Avg Gap')] += noSplit_gap                
                if(noSplit_gap > results_6dc25r.loc[nb_periods, (demand_intens, 'Cplex', 'Max Gap')]):
                    results_6dc25r.loc[nb_periods, (demand_intens, 'Cplex', 'Max Gap')] = noSplit_gap
            if(noSplit_gap <= noSplit_gap):
                results_6dc25r.loc[nb_periods, (demand_intens, 'Cplex', 'BetterOrEqual')] += 1

results_3dc10r.loc[:, ('L', 'Cplex', 'Avg Gap')] /= 9
results_3dc10r.loc[:, ('M', 'Cplex', 'Avg Gap')] /= 9
results_3dc10r.loc[:, ('H', 'Cplex', 'Avg Gap')] /= 9
results_3dc10r.loc[:, ('L', 'RS', 'Avg Gap')] /= 9
results_3dc10r.loc[:, ('M', 'RS', 'Avg Gap')] /= 9
results_3dc10r.loc[:, ('H', 'RS', 'Avg Gap')] /= 9
results_3dc10r.loc[:, ('L', 'LM + RS', 'Avg Gap')] /= 9
results_3dc10r.loc[:, ('M', 'LM + RS', 'Avg Gap')] /= 9
results_3dc10r.loc[:, ('H', 'LM + RS', 'Avg Gap')] /= 9

results_3dc25r.loc[:, ('L', 'Cplex', 'Avg Gap')] /= 9
results_3dc25r.loc[:, ('M', 'Cplex', 'Avg Gap')] /= 9
results_3dc25r.loc[:, ('H', 'Cplex', 'Avg Gap')] /= 9
results_3dc25r.loc[:, ('L', 'RS', 'Avg Gap')] /= 9
results_3dc25r.loc[:, ('M', 'RS', 'Avg Gap')] /= 9
results_3dc25r.loc[:, ('H', 'RS', 'Avg Gap')] /= 9
results_3dc25r.loc[:, ('L', 'LM + RS', 'Avg Gap')] /= 9
results_3dc25r.loc[:, ('M', 'LM + RS', 'Avg Gap')] /= 9
results_3dc25r.loc[:, ('H', 'LM + RS', 'Avg Gap')] /= 9

results_6dc25r.loc[:, ('L', 'Cplex', 'Avg Gap')] /= 9
results_6dc25r.loc[:, ('M', 'Cplex', 'Avg Gap')] /= 9
results_6dc25r.loc[:, ('H', 'Cplex', 'Avg Gap')] /= 9
results_6dc25r.loc[:, ('L', 'RS', 'Avg Gap')] /= 9
results_6dc25r.loc[:, ('M', 'RS', 'Avg Gap')] /= 9
results_6dc25r.loc[:, ('H', 'RS', 'Avg Gap')] /= 9
results_6dc25r.loc[:, ('L', 'LM + RS', 'Avg Gap')] /= 9
results_6dc25r.loc[:, ('M', 'LM + RS', 'Avg Gap')] /= 9
results_6dc25r.loc[:, ('H', 'LM + RS', 'Avg Gap')] /= 9

results_3dc10r.to_csv("resultTable_3dc10r.csv")
results_3dc10r.to_latex("table_3dc10r.tex")
results_3dc25r.to_latex("table_3dc25r.tex")
results_6dc25r.to_latex("table_6dc25r.tex")

results_3dc25r.to_csv("resultTable_3dc25r.csv")
results_6dc25r.to_csv("resultTable_6dc25r.csv")

