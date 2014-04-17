package com.alangeorge.minermonitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MinerList {

    public static List<Miner> ITEMS = new ArrayList<Miner>();

    public static Map<String, Miner> ITEM_MAP = new HashMap<String, Miner>();

    public static void addMiner(Miner item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.name, item);
    }
    
    public static boolean removeMiner(String name) {
    	Miner miner = ITEM_MAP.get(name);

    	if (miner == null) {
    		return false;
    	}
    	
    	ITEMS.remove(miner);
    	ITEM_MAP.remove(name);
    	
    	return true;
    }
    
    public void updateMinerListeners() {
    	for (Miner m : ITEMS) {
    		m.updateListeners();
    	}
    }
}
