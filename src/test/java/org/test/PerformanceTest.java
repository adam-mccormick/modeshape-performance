package org.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.MultiUseAbstractTest;
import org.modeshape.jcr.RepositoryConfiguration;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Performance test for Modeshape which shows that a mis configuration of the infinispan
 * cache can lead to lock timeout exceptions and the fix for it by using the correct
 * isolation level
 *
 */
public class PerformanceTest extends MultiUseAbstractTest{
    
    @BeforeClass
    public static void before() throws Exception{
        RepositoryConfiguration configuration = RepositoryConfiguration.read("repo.json");
        startRepository(configuration);
    }
    
    @AfterClass
    public static void after() throws Exception{
        MultiUseAbstractTest.afterAll();
        FileUtil.delete("target/data");
    }
    
    @Test
    public void shouldAllowConcurrentAccessWhenLockTimeoutIsSufficient() throws Exception{
        Session session = this.repository().login();
        session.getRootNode().addNode("cache");
        session.save();
        ExecutorService executor = Executors.newFixedThreadPool(30);
        CompletionService<String> service = new ExecutorCompletionService(executor);
        for(int i = 0; i < 1000; i++)
            service.submit(new Cacher(this.repository()));
        for(int i = 0; i < 1000; i++)
            service.take().get();
    }
    
    private static class Cacher implements Callable<String>{
        
        private final Node cache;
        
        public Cacher(final Repository repository){
            try {
                this.cache = repository.login().getNode("/cache");
            }
            catch(Exception e){
                throw new RuntimeException(e);
            }
        }
        
        @Override
        public String call() {
            try {
                String key = UUID.randomUUID().toString();
                
                try(InputStream image = Cacher.class.getResourceAsStream("/image.jpg")) {
                    Binary binary = this.cache.getSession().getValueFactory().createBinary(image);
                    Node node = this.cache.addNode(key);
                    node.setProperty("data", binary);
                    node.getSession().save();
                }
                return key;
            }
            catch(Exception e){
                throw new RuntimeException(e);
            }
        }
    }
}
