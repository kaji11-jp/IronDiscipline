package com.irondiscipline.manager;

import com.irondiscipline.IronDiscipline;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StorageManagerTest {

    @Mock
    private IronDiscipline plugin;
    @Mock
    private ConfigManager configManager;

    private StorageManager storageManager;
    private AutoCloseable mocks;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);

        when(plugin.getConfigManager()).thenReturn(configManager);
        when(configManager.getDatabaseType()).thenReturn("h2");
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("TestLogger"));

        // StorageManager initializes DB in constructor
        storageManager = new StorageManager(plugin);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (storageManager != null) {
            storageManager.shutdown();
        }
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    void testGetArmorBackupAsync() {
        UUID playerId = UUID.randomUUID();
        CompletableFuture<String> future = storageManager.getArmorBackupAsync(playerId);
        assertNotNull(future, "Async method should return a future");
        assertNull(future.join(), "Future should complete with null for non-existent player");
    }

    @Test
    void testGetArmorBackupBlocking() {
        UUID playerId = UUID.randomUUID();
        String result = storageManager.getArmorBackup(playerId);
        assertNull(result, "getArmorBackup should return null for non-existent player");
    }

    @Test
    void testCachingBehavior() {
        UUID playerId = UUID.randomUUID();
        String armorData = "ArmorBackupData";
        String invData = "InventoryBackupData";
        String location = "world;0;0;0;0;0";

        // 1. Save data (should populate cache)
        boolean saved = storageManager.saveJailedPlayerAsync(playerId, "TestPlayer", "TestReason",
                null, location, invData, armorData).join();
        assertTrue(saved, "Save should succeed");

        // 2. Fetch using blocking method (should be fast/cached)
        String cachedArmor = storageManager.getArmorBackup(playerId);
        assertEquals(armorData, cachedArmor, "Should retrieve cached armor data");

        String cachedInv = storageManager.getInventoryBackup(playerId);
        assertEquals(invData, cachedInv, "Should retrieve cached inventory data");

        // 3. Remove data (should clear cache)
        storageManager.removeJailedPlayerAsync(playerId).join();

        // 4. Fetch again (should be null)
        assertNull(storageManager.getArmorBackup(playerId), "Should be null after removal");
    }
}
