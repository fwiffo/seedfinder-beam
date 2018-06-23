package minecraft.layer;

import com.google.common.collect.Lists;
import java.lang.ThreadLocal;
import java.util.List;
import java.util.ArrayList;

public class IntCache
{
    private static final ThreadLocal<Integer> intCacheSize =
        ThreadLocal.withInitial(() -> 256);

    /**
     * A list of pre-allocated int[256] arrays that are currently unused and can be returned by getIntCache()
     */
    private static ThreadLocal<List<int[]>> freeSmallArrays =
        ThreadLocal.withInitial(() -> new ArrayList<int[]>());

    /**
     * A list of pre-allocated int[256] arrays that were previously returned by getIntCache() and which will not be re-
     * used again until resetIntCache() is called.
     */
    private static ThreadLocal<List<int[]>> inUseSmallArrays =
        ThreadLocal.withInitial(() -> new ArrayList<int[]>());

    /**
     * A list of pre-allocated int[cacheSize] arrays that are currently unused and can be returned by getIntCache()
     */
    private static ThreadLocal<List<int[]>> freeLargeArrays =
        ThreadLocal.withInitial(() -> new ArrayList<int[]>());

    /**
     * A list of pre-allocated int[cacheSize] arrays that were previously returned by getIntCache() and which will not
     * be re-used again until resetIntCache() is called.
     */
    private static ThreadLocal<List<int[]>> inUseLargeArrays =
        ThreadLocal.withInitial(() -> new ArrayList<int[]>());

    public static synchronized int[] getIntCache(int p_76445_0_)
    {
        int[] var1;

        if (p_76445_0_ <= 256)
        {
            if (freeSmallArrays.get().isEmpty())
            {
                var1 = new int[256];
                inUseSmallArrays.get().add(var1);
                return var1;
            }
            else
            {
                var1 = (int[])freeSmallArrays.get().remove(freeSmallArrays.get().size() - 1);
                inUseSmallArrays.get().add(var1);
                return var1;
            }
        }
        else if (p_76445_0_ > intCacheSize.get())
        {
            intCacheSize.set(p_76445_0_);
            freeLargeArrays.get().clear();
            inUseLargeArrays.get().clear();
            var1 = new int[intCacheSize.get()];
            inUseLargeArrays.get().add(var1);
            return var1;
        }
        else if (freeLargeArrays.get().isEmpty())
        {
            var1 = new int[intCacheSize.get()];
            inUseLargeArrays.get().add(var1);
            return var1;
        }
        else
        {
            var1 = (int[])freeLargeArrays.get().remove(freeLargeArrays.get().size() - 1);
            inUseLargeArrays.get().add(var1);
            return var1;
        }
    }

    /**
     * Mark all pre-allocated arrays as available for re-use by moving them to the appropriate free lists.
     */
    public static synchronized void resetIntCache()
    {
        if (!freeLargeArrays.get().isEmpty())
        {
            freeLargeArrays.get().remove(freeLargeArrays.get().size() - 1);
        }

        if (!freeSmallArrays.get().isEmpty())
        {
            freeSmallArrays.get().remove(freeSmallArrays.get().size() - 1);
        }

        freeLargeArrays.get().addAll(inUseLargeArrays.get());
        freeSmallArrays.get().addAll(inUseSmallArrays.get());
        inUseLargeArrays.get().clear();
        inUseSmallArrays.get().clear();
    }

    /**
     * Gets a human-readable string that indicates the sizes of all the cache fields.  Basically a synchronized static
     * toString.
     */
    public static synchronized String getCacheSizes()
    {
        return "cache: " + freeLargeArrays.get().size() + ", tcache: " + freeSmallArrays.get().size() + ", allocated: " + inUseLargeArrays.get().size() + ", tallocated: " + inUseSmallArrays.get().size();
    }
}
