package com.atguigu.gmall.index;

import com.google.common.base.Charsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.bouncycastle.util.encoders.UTF8;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GmallIndexApplicationTests {

    @Test
    void contextLoads() {

        BloomFilter<CharSequence> bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_8), 10, 0.3);
        bloomFilter.put("1");
        bloomFilter.put("2");
        bloomFilter.put("3");
        bloomFilter.put("4");
        bloomFilter.put("5");

        System.out.println(bloomFilter.mightContain("1"));
        System.out.println(bloomFilter.mightContain("11"));
        System.out.println(bloomFilter.mightContain("111"));
        System.out.println(bloomFilter.mightContain("112"));
        System.out.println(bloomFilter.mightContain("113"));
        System.out.println(bloomFilter.mightContain("114"));
        System.out.println(bloomFilter.mightContain("115"));
        System.out.println(bloomFilter.mightContain("116"));
        System.out.println(bloomFilter.mightContain("117"));

    }

}
