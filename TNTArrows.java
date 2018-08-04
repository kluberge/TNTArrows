package me.kluberge.tntarrows;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class TNTArrows extends JavaPlugin implements Listener{
	public static ItemStack tntarrow, stickytntarrow, pouch; //zombieChest, skeleChest;
	private Map<UUID,FiringMode> modes = new HashMap<>();
	private Map<UUID, Inventory> invs = new HashMap<>();
	private Map<Integer,Integer> tntArrowID = new HashMap<>(); 
	private List<Integer> sticky = new ArrayList<>();
	private List<Integer> tntarrs = new ArrayList<>();
	private ConfigAccessor invFile;
	
	@Override
	public void onEnable(){
		initItems();
		setupRecipe();
		
		getServer().getPluginManager().registerEvents(this, this);
		
		invFile = new ConfigAccessor(this, "invs.yml");
		
		//setup invs
		FileConfiguration fc = invFile.getConfig();
		for(String id : fc.getKeys(false))
		{
			UUID u = UUID.fromString(id);
			Inventory i = getServer().createInventory(null, 9);
			for(int x=0; x<9; x++)
			{
				i.addItem(fc.getItemStack(id+"."+x));
			}
			invs.put(u, i);
		}
		
		for(Player p : Bukkit.getOnlinePlayers())
		{
			modes.put(p.getUniqueId(), FiringMode.REG);
		}
		
	}
	
	@Override
	public void onDisable(){
		saveInvs();
		tntArrowID.clear();
		tntarrs.clear();
		sticky.clear();
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		if(label.equalsIgnoreCase("tntarrow"))
		{
			if(args.length!=4||!args[0].equalsIgnoreCase("give")){
				sender.sendMessage("Usage: /tntarrow give <playername> <type> <amount>");
			}
			else
			{
				Player p = (Player) Bukkit.getPlayer(args[1]);
				int amt = 0;
				try{
					amt = Integer.parseInt(args[3]);
				}catch(Exception e){Bukkit.getLogger().info("exception caught");return false;}
				ItemStack i = null;
				if(args[2].equalsIgnoreCase("tnt"))
					i=tntarrow.clone();
				else if(args[2].equalsIgnoreCase("stickytnt"))
					i=stickytntarrow.clone();
				i.setAmount(amt);
				p.getInventory().addItem(i);
			}
				
			return true;
		}
		
		
		return false;
	}
	
	/*private void spawnZombie(Location l, LivingEntity e)
	{
		WorldServer s = ((CraftWorld) l.getWorld()).getHandle();
		FriendlyZombie z = new FriendlyZombie(s,e);
		z.setPositionRotation(l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
		z.canPickUpLoot = false;
		net.minecraft.server.v1_12_R1.ItemStack chest = CraftItemStack.asNMSCopy(zombieChest);
		z.setEquipment(EnumItemSlot.CHEST, chest);
	}*/

	private void setupRecipe() {
		ShapelessRecipe tnt = new ShapelessRecipe(new NamespacedKey(this, "tntarrow"), tntarrow);
		tnt.addIngredient(Material.ARROW);
		tnt.addIngredient(Material.TNT);
		getServer().addRecipe(tnt);
		
		ShapelessRecipe stnt1 = new ShapelessRecipe(new NamespacedKey(this, "stickytntarrow"), stickytntarrow);
		stnt1.addIngredient(Material.ARROW);
		stnt1.addIngredient(Material.TNT);
		stnt1.addIngredient(Material.SLIME_BALL);
		getServer().addRecipe(stnt1);
		
	}

	private void initItems() {
		ItemStack tntarrow = new ItemStack(Material.ARROW);
		ItemMeta m = tntarrow.getItemMeta();
		m.setDisplayName(ChatColor.RED+"TNT Arrow");
		ArrayList<String> lore = new ArrayList<>();
		lore.add("Ignites on impact");
		m.setLore(lore);
		tntarrow.setItemMeta(m);
	    TNTArrows.tntarrow = tntarrow;
	    
	    ItemStack sta = new ItemStack(Material.ARROW);
	    ItemMeta m4 = sta.getItemMeta();
	    m4.setDisplayName(ChatColor.RED+"Sticky TNT Arrow");
	    lore = new ArrayList<>();
	    lore.add("TNT sticks where"); 
	    lore.add("arrow hits");
	    m4.setLore(lore);
	    sta.setItemMeta(m4);
	    stickytntarrow = sta;
		
		ItemStack pouch = new ItemStack(Material.LEATHER);
		ItemMeta m2 = pouch.getItemMeta();
		m2.setDisplayName(ChatColor.YELLOW+"Arrow Pouch");
		lore = new ArrayList<>();
		lore.add("Stores arrows");
		m2.setLore(lore);
		pouch.setItemMeta(m2);
		TNTArrows.pouch = pouch;
		
		/*ItemStack zombieChest = new ItemStack(Material.LEATHER_CHESTPLATE);
		ItemMeta m3 = zombieChest.getItemMeta();
		LeatherArmorMeta lm = (LeatherArmorMeta) m3;
		lm.setColor(Color.FUCHSIA);
		lm.setDisplayName(ChatColor.GOLD+"Good Zombie");
		zombieChest.setItemMeta(lm);*/
	}
	
	public void saveInvs(){
		FileConfiguration fc = invFile.getConfig();
		for(Entry<UUID,Inventory> e : invs.entrySet())
		{
			for(int i=0; i<e.getValue().getSize(); i++)
			{
				fc.set(e.getKey()+"."+i, e.getValue().getItem(i));
			}
		}
	}
	
	@EventHandler
	public void onShootBow(EntityShootBowEvent e)
	{
		Entity en = e.getEntity();
		final Projectile a = (Projectile) e.getProjectile();
		if(en.getType()==EntityType.PLAYER)
		{
			final Player p = (Player) en;
			if(p.getInventory().containsAtLeast(tntarrow, 1)&&modes.get(p.getUniqueId())==FiringMode.TNT
					||p.getInventory().containsAtLeast(stickytntarrow, 1)&&modes.get(p.getUniqueId())==FiringMode.STICKY_TNT)
			{
				for(ItemStack is : p.getInventory().getContents())
					if(is!=null && (is.isSimilar(tntarrow)&&modes.get(p.getUniqueId())==FiringMode.TNT||is.isSimilar(stickytntarrow)&&modes.get(p.getUniqueId())==FiringMode.STICKY_TNT))
					{
						if(is.getAmount()>1)
							is.setAmount(is.getAmount()-1);
						else
							p.getInventory().remove(modes.get(p.getUniqueId())==FiringMode.TNT?tntarrow:stickytntarrow);
						break;
					}
				Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
					@Override
					public void run() {if(!p.getInventory().containsAtLeast(tntarrow,1)){
						modes.put(p.getUniqueId(), FiringMode.REG);
						notifyNewMode(p,FiringMode.REG);
					}}
				}, 1L);
				tntarrs.add(a.getEntityId());
				/*Bukkit.getScheduler().scheduleSyncDelayedTask(this, ()->{
					TNTPrimed tnt = (TNTPrimed) a.getWorld().spawnEntity(a.getLocation().add(new Vector(0,2,0)), EntityType.PRIMED_TNT);
					tnt.setGlowing(false);
					tnt.setFuseTicks(Integer.MAX_VALUE);
					a.setVelocity(a.getVelocity().setY(a.getVelocity().getY()>0d?0d:a.getVelocity().getY()));
					if(!a.isOnGround())a.setPassenger(tnt);}, 
						1L);*/
				//determine if arrow velocity has y component
				int sid = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
					@Override
					public void run() {
						if(a.getVelocity().getY()<=0
								&& a.getPassengers().size() == 0 
								&& !a.isOnGround())
						{
							TNTPrimed tnt = (TNTPrimed) a.getWorld().spawnEntity(a.getLocation().add(new Vector(0,2,0)), EntityType.PRIMED_TNT);
							tnt.setFuseTicks(Integer.MAX_VALUE);
							a.addPassenger(tnt);
							Bukkit.getScheduler().cancelTask(tntArrowID.get(a.getEntityId()));
						}
					}
				}, 1L, 1L);
				int aid = a.getEntityId();
				tntArrowID.put(aid, sid);
				if(modes.get(p.getUniqueId()) == FiringMode.STICKY_TNT)
					sticky.add(aid);
			}
		}
	}
	
	private void notifyNewMode(Player p, FiringMode ms) {
		p.sendMessage("Now using "+ms.getName()+" Arrows");
		p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 0.5f, 1f);
		p.playEffect(p.getLocation(), Effect.BLAZE_SHOOT, null);
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e)
	{
		Player p = e.getPlayer();
		modes.put(p.getUniqueId(), FiringMode.REG);
	}
	
	@EventHandler
	public void onLeftClick(PlayerInteractEvent e){
		Player p = e.getPlayer();
		Inventory i = p.getInventory();
		if((e.getAction()==Action.LEFT_CLICK_AIR||e.getAction()==Action.LEFT_CLICK_BLOCK)&&e.getItem()!=null&&e.getItem().getType()==Material.BOW){
			//cycle through modes
			List<FiringMode> possible = new ArrayList<>();
			possible.add(FiringMode.REG);
			for(ItemStack is : i.getContents())
				if(is!=null&&is.isSimilar(tntarrow))
				{
					possible.add(FiringMode.TNT);
					break;
				}
			
			for(ItemStack is : i.getContents())
				if(is!=null&&is.isSimilar(stickytntarrow))
				{
					possible.add(FiringMode.STICKY_TNT);
					break;
				}
			int next = (possible.indexOf(modes.get(p.getUniqueId()))+1)%possible.size();
			FiringMode ms = possible.get(next);
			modes.put(p.getUniqueId(), possible.get(next));
			this.notifyNewMode(p, ms);
		}
	}
	
	@EventHandler
	public void onProjectileHit(ProjectileHitEvent e){
		if(tntarrs.contains(e.getEntity().getEntityId()))
		{
			boolean found = false;
			for(int i=0; i<tntarrs.size(); i++)
				if(tntarrs.get(i)==e.getEntity().getEntityId())
				{
					tntarrs.remove(i);
					found = true;
				}
			if(!found)
				return;
			Entity en = e.getEntity();
			List<Entity> riding = en.getPassengers();
			if(riding.size() != 0)
			{
				en.eject();
				for(int i=0; i<riding.size(); i++)
					riding.get(i).remove();
			}
			TNTPrimed tnt = (TNTPrimed) e.getEntity().getWorld().spawnEntity(e.getEntity().getLocation(), EntityType.PRIMED_TNT);
			tnt.setGlowing(true);
			if(sticky.contains(e.getEntity().getEntityId()))
			{
				tnt.setGravity(false);
				tnt.setVelocity(new Vector(0,0,0));
				Entity he = e.getHitEntity();
				if(he != null)
					he.addPassenger(tnt);
			}
			sticky.remove(Integer.valueOf(e.getEntity().getEntityId()));
			tntArrowID.remove(e.getEntity().getEntityId());
		}
	}
	
	/*@EventHandler
	public void onTNTExplode(EntityExplodeEvent e)
	{
		if(!(e.getEntity() instanceof TNTPrimed))
			return;
		//TODO
	}*/
	
	
	@EventHandler
	public void onInventoryUpdate(InventoryEvent e)
	{
		//TODO: make sure arrows cannot be stacked, make sure the pouches cannot be stacked
	}
	
	public enum FiringMode{
		REG("Regular"),TNT("TNT"),STICKY_TNT("Sticky TNT");//,ZOMB("Zombie"),SKELE("Skeleton"),SPID("Spider"),TELE("Teleport"),LIGHT("Lightning"),FIRE("Fire"),WEB("Spider Web"),FRAG("Fragmentation"),BURST("Burst");
		private String name;
		private FiringMode(String name)
		{
			this.name = name;
		}
		
		public String getName()
		{
			return name;
		}
	}
}
