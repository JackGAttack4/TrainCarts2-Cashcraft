package com.movies22.cashcraft.tc.signactions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.type.Jigsaw;
import org.bukkit.block.data.type.Jigsaw.Orientation;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.sl.API.Variables;
import com.movies22.cashcraft.tc.TrainCarts;
import com.movies22.cashcraft.tc.api.MinecartGroup;
import com.movies22.cashcraft.tc.api.MinecartMember;
import com.movies22.cashcraft.tc.api.Station;
import com.movies22.cashcraft.tc.controller.PisController;
import com.movies22.cashcraft.tc.utils.Guides;
import com.movies22.cashcraft.tc.utils.StationAnnouncements;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;


public class SignActionPlatform extends SignAction {
	public String platform = null;
	public String name;
	public Station station;
	public long duration;
	public Vector offset;
	public int doors;
	private String n;
	public List<Location> doorLocs = new ArrayList<Location>();
	public List<Location> lightLocs = new ArrayList<Location>();
	public boolean inverted = false;
	private Character headcode;
	public boolean reverse = false;
	public HashMap<Character, PisController.PIS> pis = new HashMap<Character,PisController.PIS>();
	
	@Override
	public SignActionPlatform clone() {
		SignActionPlatform a = new SignActionPlatform();
		a.platform = null;
		a.name = "";
		a.station = null;
		a.duration = 0;
		a.offset = null;
		a.doors = 0;
		a.n = "";
		a.doorLocs = new ArrayList<Location>();
		a.lightLocs = new ArrayList<Location>();
		a.inverted = false;
		a.headcode = null;
		a.reverse = false;
		a.pis = new HashMap<Character,PisController.PIS>();
		return a;	
	}
	
	public void setLights(Material light) {
		this.lightLocs.forEach(loc -> {
			loc.getBlock().setType(light);
		});
		if(light.equals(Material.VERDANT_FROGLIGHT)) {
			this.doorLocs.forEach(loc -> {
				loc.clone().subtract(0, 1, 0).getBlock().setType(Material.REDSTONE_TORCH);
			});
		} else if(light.equals(Material.PEARLESCENT_FROGLIGHT)) {
			this.doorLocs.forEach(loc -> {
				loc.clone().subtract(0, 1, 0).getBlock().setType(Material.AIR);
			});
		}
	}
	SignActionPlatform b;
	public Timer groupAnnounceTask;
	int stops = 0;
	
	Timer a;
	Timer a2;
	public Boolean execute(MinecartGroup group) {
		b = null;
		if(a2 != null) {
			a2.cancel();
		}
		if(this.station.closed) {
			group.currentRoute.stops.remove(0);
			return true;
		}
		if(group.currentRoute.stops.size() > 0 && group.currentRoute.stops.get(0).equals(this)) {
			stops = group.currentRoute.stops.size();
			if(group.currentRoute.stops.size() <= 1) {
				if(this.reverse) {
					group.loadNextRoute(false, true);
					group.reverse();
					group.getMembers().forEach(m -> {
						m.proceedTo(this.node.getLocation());
					});
				} else {
					group.loadNextRoute(true);
				}
			}
			headcode = group.getHeadcode().charAt(1);
			group.getMembers().forEach(m -> {
				m.currentSpeed = 0.0;
				m._targetSpeed = 0.0;
			});
			PisController.PIS pis;
			if(this.pis.get(headcode) != null) {
				pis = this.pis.get(headcode);
			} else {
				pis = TrainCarts.plugin.PisController.getPis(this.station.code + this.platform + headcode);
				this.pis.put(headcode, pis);
			}
			long dur = this.duration;
			pis.setArrived(true);
			long dur2 = 0L;
			if((pis.delay % group.nextTrain) > 0) {
				if((pis.delay % group.nextTrain) < dur/2) {
					dur = dur - (pis.delay % group.nextTrain);
				
				} else {
					dur = dur/2;
					dur2 = (pis.delay % group.nextTrain) - dur/2;
				}
			}
			if(pis.delay < 0 && pis.delay > (-dur/2)) {
				dur = dur + -pis.delay;
			} else if(pis.delay < 0) {
				dur = dur*3/2;
				dur2 = (pis.delay % group.nextTrain) + dur/2;
			}
			TrainCarts.plugin.PisController.getPis(this.station.code + this.platform + headcode).addTimer(group.nextTrain - (int) dur2);
			pis.delay = 0;
			pis = null;
		n = group.currentRoute.name;
		this.setLights(Material.VERDANT_FROGLIGHT);
		List<String> ann = new ArrayList<String>();
		String c = group.getLine().getChar();
		ann.add("This station is " + this.station.name + ".");
		if(this.station.osi != c && this.station.osi != "") {
			ann.add(StationAnnouncements.parseMetro(this.station.osi, group.getLine()));
		}
		if(this.station.hosi != null && !this.station.hosi.equals("")) {
			ann.add(StationAnnouncements.parseRail(this.station.hosi, group.getLine(), (ann.size() > 1)));
		}
		if(this.station.station != "") {
			String s = this.station.generateConnection(group.getLine());
			ann.add(s);
		} 
		if(stops == 1) {
			ann.add("This train terminates here. All change please.");
		}
		group.announce(ann.get(0), false, ann.get(0).contains("{\"text"));
		ann.remove(0);
		if(!group.isEmpty) {
		groupAnnounceTask = new Timer();
		groupAnnounceTask.schedule( 
		        new java.util.TimerTask() {
		            @Override
		            public void run() {
		            	if(ann.size() > 0) {
		            		if(ann.get(0) != null) {
		            			group.announce(ann.get(0), false, ann.get(0).contains("{\"text"));
		            		};	 
		            		ann.remove(0);
		            	} else {
		            		this.cancel();
		            		groupAnnounceTask = null;
		            	}
		            }
		        }, 
		        2500L, 2500L
		);
		}
		a = new java.util.Timer();
		a.schedule( 
		        new java.util.TimerTask() {
		            @Override
		            public void run() {
		            	depart(group);
		                group.getMembers().forEach(m -> {
		                	m._targetSpeed = 0.6;
		                	m._mod = 1.0;
		                });
		            }
		        }, 
		        dur*1000
		);
    	return true;
		} else {
			group.head().proceedTo(this.node.loc);
			return true;
		}
    }
	
	public void depart(MinecartGroup g) {
		if(a != null) {
			a.cancel();
		}
		a = null;
		if(g.currentRoute.stops.size() > 0 && g.currentRoute.stops.get(0).equals(this)) {
			g.currentRoute.stops.remove(0);
		}
		if(TrainCarts.plugin.PisController != null) {
			TrainCarts.plugin.PisController.getPis(this.station.code + this.platform + headcode).setArrived(false);
		}
		return;
	}
	public Boolean exit(MinecartGroup group) {
		this.setLights(Material.PEARLESCENT_FROGLIGHT);
		if(group.currentRoute.name.equals("DESPAWN")) {
			group.destroy();
		}
		a2 = new java.util.Timer();
		if(!group.currentRoute._line.getName().equals("#GLOBAL") && group.currentRoute.stops.size() > 0) {
			group.announce("This is a " + group.currentRoute._line.getName() + " Line service to " + group.currentRoute.stops.get(group.currentRoute.stops.size() - 1).station.name + ".");
		} else {
			group.announce(group + " - " + group.getHeadcode() + " (why are you still on this train lmao)");
			group.eject();
			group.destroy();
		}
		if(!group.currentRoute.name.equals("[CACHED ROUTE]")) {
			TimerTask t = new java.util.TimerTask() {
	            @Override
	            public void run() {
	            	if(group.currentRoute.stops.size() > 0) {
	            		if(group.currentRoute.stops.get(0).station.closed) {
	            			group.announce("The next station is closed.");
	            		} else {
	            			group.announce("The next station is " + group.currentRoute.stops.get(0).station.name + ".");
	            		}
	            		}
	            	}
	        	};
			a2.schedule(t, 3000
					);
				
		}
		return true;
	}
	
	@Override
	public void postParse() {
		try {
		String[] a = this.content.split(" ");
		this.platform = a[1];
		Station b = TrainCarts.plugin.StationStore.getFromCode(a[2]);
		if(b == null) {
			TrainCarts.plugin.getLogger().log(Level.WARNING, this.content + " is an invalid SignActionPlatform sign.");
			this.platform = null;
			this.node.line.deleteNode(this.node);
			if(this.node.line != TrainCarts.plugin.global) {
				TrainCarts.plugin.global.deleteNode(this.node);
			}
			return;
		}
		this.station = b;
		b.addPlatform(a[1], this);
		try {
			this.duration = Long.parseLong(a[3]);
		} catch(NumberFormatException e) {
			TrainCarts.plugin.getLogger().log(Level.WARNING, this.content + " is an invalid SignActionPlatform sign.");
			this.platform = null;
			this.node.line.deleteNode(this.node);
			if(this.node.line != TrainCarts.plugin.global) {
				TrainCarts.plugin.global.deleteNode(this.node);
			}
			return;
		}
		String[] c = a[4].split("/");
		if(a.length > 6) {
			if(a[6].equals("R")) {
				this.reverse = true;
				TrainCarts.plugin.getLogger().log(Level.INFO, this.station.code + "~" + this.platform + " reverses.");
			}
		
		}
		this.offset = new Vector(Integer.valueOf(c[0]),Integer.valueOf(c[1]),Integer.valueOf(c[2]));
		Vector offset;
		Vector addition;
		switch(this.node.direction) {
		case EAST:
			offset = new Vector(0, 1, -2);
			addition = new Vector(3, 0, 0);
			break;
		case NORTH:
			offset = new Vector(-2, 1, 0);
			addition = new Vector(0, 0, -3);
			break;
		case SOUTH:
			offset = new Vector(2, 1, 0);
			addition = new Vector(0, 0, 3);
			break;
		case WEST:
			offset = new Vector(0, 1, 2);
			addition = new Vector(-3, 0, 0);
			break;
		default:
			offset = new Vector(0, 0, 0);
			addition = new Vector(0, 0, 0);
			this.doors = 0;
			break;
		}
		this.doors = Integer.valueOf(a[5]);
		Location z = this.sign.getBlock().getLocation().clone();
		Vector addition2 = addition.clone().divide(new Vector(3, 3, 3));
		Location light = z.subtract(offset).add(0, 2, 0);
		if(light.getBlock().getType().equals(Material.JIGSAW) || light.getBlock().getType().equals(Material.PUMPKIN) || light.getBlock().getType().equals(Material.VERDANT_FROGLIGHT) || light.getBlock().getType().equals(Material.PEARLESCENT_FROGLIGHT)) {
			doorLocs = new ArrayList<Location>();
			lightLocs = new ArrayList<Location>();
			lightLocs.add(light.clone().subtract(addition2));
			for(int i = 0; i < this.doors; i++) {
				if(light.getBlock().getType().equals(Material.JIGSAW) || light.getBlock().getType().equals(Material.PUMPKIN) || light.getBlock().getType().equals(Material.VERDANT_FROGLIGHT) || light.getBlock().getType().equals(Material.PEARLESCENT_FROGLIGHT)) {
					doorLocs.add(light.clone());
					light.clone().subtract(0,  1,  0).getBlock().setType(Material.AIR);
				} else {
					break;
				}
				light.getBlock().setType(Material.JIGSAW);
				Jigsaw z2 = (Jigsaw) light.getBlock().getBlockData();
				z2.setOrientation(Orientation.valueOf(this.node.direction.name() + "_UP"));
				light.getBlock().setBlockData(z2);
				
				Location z3 = light.clone().add(addition2);
				z3.getBlock().setType(Material.JIGSAW);
				Jigsaw z4 = (Jigsaw) z3.getBlock().getBlockData();
				z4.setOrientation(Orientation.valueOf(this.node.direction.getOppositeFace().name() + "_UP"));
				z3.getBlock().setBlockData(z4);
				light.add(addition);
				light.subtract(addition2);
				lightLocs.add(light.clone());
				light.add(addition2);
			}
			lightLocs.forEach(loc -> {
				loc.getBlock().setType(Material.PEARLESCENT_FROGLIGHT);
			});
			return;
		} 
		light.add(offset).add(offset).subtract(0,  2,  0);
		if(light.getBlock().getType().equals(Material.JIGSAW) || light.getBlock().getType().equals(Material.PUMPKIN) || light.getBlock().getType().equals(Material.VERDANT_FROGLIGHT) || light.getBlock().getType().equals(Material.PEARLESCENT_FROGLIGHT)) {
			doorLocs = new ArrayList<Location>();
			lightLocs = new ArrayList<Location>();
			lightLocs.add(light.clone().subtract(addition2));
			for(int i = 0; i < this.doors; i++) {
				if(light.getBlock().getType().equals(Material.JIGSAW) || light.getBlock().getType().equals(Material.PUMPKIN) || light.getBlock().getType().equals(Material.VERDANT_FROGLIGHT) || light.getBlock().getType().equals(Material.PEARLESCENT_FROGLIGHT)) {
					doorLocs.add(light.clone());
					light.clone().subtract(0,  1,  0).getBlock().setType(Material.AIR);
				} else {
					break;
				}
				light.getBlock().setType(Material.JIGSAW);
				Jigsaw z2 = (Jigsaw) light.getBlock().getBlockData();
				z2.setOrientation(Orientation.valueOf(this.node.direction.name() + "_UP"));
				light.getBlock().setBlockData(z2);
				
				Location z3 = light.clone().add(addition2);
				z3.getBlock().setType(Material.JIGSAW);
				Jigsaw z4 = (Jigsaw) z3.getBlock().getBlockData();
				z4.setOrientation(Orientation.valueOf(this.node.direction.getOppositeFace().name() + "_UP"));
				z3.getBlock().setBlockData(z4);
				light.add(addition);
				light.subtract(addition2);
				lightLocs.add(light.clone());
				light.add(addition2);
			}
			lightLocs.forEach(loc -> {
				loc.getBlock().setType(Material.PEARLESCENT_FROGLIGHT);
			});
		
			return;
		}
		} catch(IndexOutOfBoundsException e) {
			TrainCarts.plugin.getLogger().log(Level.WARNING, this.content + " is an invalid SignActionPlatform sign.");
			this.platform = null;
			this.node.line.deleteNode(this.node);
			if(this.node.line != TrainCarts.plugin.global) {
				TrainCarts.plugin.global.deleteNode(this.node);
			}
			return;
		}
    	return;
    }
	
	@Override
	public Boolean match(String s) {
    	return s.toLowerCase().equals("t:plat");
    }
	
	@Override
	public String getAction() {
		return "SignActionPlatform";
    }
	
	@Override
	public Double getSpeedLimit(MinecartGroup g) {
		if(!this.station.closed) {
			return 0.0;
		} else {
			return null;
		}
    }
	
	@Override
	public void handleBuild(Player p) {
		TextComponent m1 = new TextComponent(ChatColor.YELLOW + "You've built a ");
		TextComponent clickable = new TextComponent(ChatColor.BLUE + "" + ChatColor.UNDERLINE + "PLATFORM");
		TextComponent m2 = new TextComponent(ChatColor.YELLOW + " sign.");
		TextComponent m3 = new TextComponent(ChatColor.GREEN + "\nUse this sign to make a train stop at a station.");
		clickable.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, Guides.GUIDE_LINK.id + Guides.PLATFORM_SIGN.id));
		p.spigot().sendMessage(m1, clickable, m2, m3);
	}
}
