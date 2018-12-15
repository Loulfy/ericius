package io.lylix.etb;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.border.WorldBorder;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Mod.EventBusSubscriber
@Mod(modid = ETB.MODID, name = ETB.NAME, version = "${version}", serverSideOnly = true, acceptableRemoteVersions = "*")
public class ETB
{
    public static final String MODID = "etb";
    public static final String NAME = "ETB";
    public static final String SPAWN = "etb-spawn.txt";

    @Mod.Instance
    public static ETB instance;

    private static Logger logger;

    private static Map<Integer, BlockPos> spawn = new HashMap<>();

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
        parse(event.getModConfigurationDirectory());
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {

    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        WorldServer world = DimensionManager.getWorld(event.toDim);
        WorldBorder border = world.getWorldBorder();
        if(border.getClosestDistance(event.player) < 0)
        {
            ETB.logger.info("WorldBorderRedirect : {} from {} to {}", event.player.getName(), event.fromDim, event.toDim);
            MinecraftServer s = FMLCommonHandler.instance().getMinecraftServerInstance();
            s.getPlayerList().transferPlayerToDimension((EntityPlayerMP) event.player, event.toDim, new TeleportToSpawn());
        }
    }

    public void parse(File dir)
    {
        File file = new File(dir, SPAWN);
        Properties props = new Properties();

        try
        {
            if(file.createNewFile()) logger.warn("create spawn file");
            props.load(new FileInputStream(file));
        }
        catch (IOException e)
        {
            logger.warn("can not parse spawn file");
        }

        for(String key : props.stringPropertyNames())
        {
            try
            {
                final String[] coords = props.getProperty(key).split(",");
                if(coords.length < 3) continue;


                int d = Integer.valueOf(key);
                int x = Integer.valueOf(coords[0]);
                int y = Integer.valueOf(coords[1]);
                int z = Integer.valueOf(coords[2]);

                BlockPos p = new BlockPos(x,y,z);
                logger.info("register spawn : {} to {}", d, p);
                spawn.put(d, p);
            }
            catch(NumberFormatException e)
            {
                logger.warn("can not completely parse the spawn file : {}", props.getProperty(key));
            }
        }
    }

    public BlockPos getSpawn(World world)
    {
        return spawn.getOrDefault(world.provider.getDimension(), world.getSpawnPoint());
    }
}

class TeleportToSpawn implements ITeleporter
{

    @Override
    public void placeEntity(World world, Entity entity, float yaw)
    {
        BlockPos p = ETB.instance.getSpawn(world);
        entity.setLocationAndAngles(p.getX(), p.getY(), p.getZ(), entity.rotationYaw, 0.0F);
        entity.motionX = 0.0D;
        entity.motionY = 0.0D;
        entity.motionZ = 0.0D;
        entity.sendMessage(new TextComponentString("Redirected to spawn..."));
    }

    @Override
    public boolean isVanilla()
    {
        return false;
    }
}
