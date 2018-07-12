/*  
Notice from the C code that this class is based on.

Written in 2014-2015 by Sebastiano Vigna (vigna@acm.org)

To the extent possible under law, the author has dedicated all copyright
and related and neighboring rights to this software to the public domain
worldwide. This software is distributed without any warranty.

See <http://creativecommons.org/publicdomain/zero/1.0/>. */

package cz.cas.mbu.genexpi.compute;

import java.security.SecureRandom;
import java.util.SplittableRandom;

public class XorShift1024 {

    private final long s[] = new long[16]; 
    private int p = 0;

    public void InitFromSecureRandomAndSplitMix()
    {
        SecureRandom baseRandom = new SecureRandom();
        //get only 64bits of true randomness to not deplete randomness resources
        byte[] seed = baseRandom.generateSeed(8);
        //convert 8 bytes to long
        long splitSeed = 0;
        for (int i = 0; i < 8; i++) {
            splitSeed <<= 8;
            splitSeed |= (seed[i] & 0xFF);
        }
        //And get the rest of state with SplitMix (as per reccomendation of XorShift authors)
        SplittableRandom splitRandom = new SplittableRandom(splitSeed);
        for(int i = 0; i < 16; i++)
        {
            s[i] = splitRandom.nextLong();
        }
    }
    
	public void InitFromFixedSeed(long fixedSeed) {
		//Seed with splitMix
		SplittableRandom rand = new SplittableRandom(fixedSeed);
        for(int i = 0; i < 16; i++)
        {
            s[i] = rand.nextLong();
        }
	}
    
    
    public long[] GetState()
    {
        return s;
    }
    
    public long Next() {
	final long s0 = s[p];
	long s1 = s[p = (p + 1) & 15];
	s1 ^= s1 << 31; // a
	s[p] = s1 ^ s0 ^ (s1 >> 11) ^ (s0 >> 30); // b,c
	return s[p] * 1181783497276652981L;
    }


/* This is the jump function for the generator. It is equivalent
   to 2^512 calls to next(); it can be used to generate 2^512
   non-overlapping subsequences for parallel computations. */

	private static final long JUMP[] = { 0x84242f96eca9c41dL,
		0xa3c65b8776f96855L, 0x5b34a39f070b5837L, 0x4489affce4f31a1eL,
		0x2ffeeb0a48316f40L, 0xdc2d9891fe68c022L, 0x3659132bb12fea70L,
		0xaac17d8efa43cab8L, 0xc4cb815590989b13L, 0x5ee975283d71c93bL,
		0x691548c86c1bd540L, 0x7910c41d10a1e6a5L, 0x0b5fc64563b3e2a8L,
		0x047f7684e9fc949dL, 0xb99181f2d8f685caL, 0x284600e3f30e38c3L};
    
    public void Jump() {

	long t[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	for(int i = 0; i < JUMP.length; i++)
		for(int b = 0; b < 64; b++) {
			if ((JUMP[i] & 1L << b) != 0)
				for(int j = 0; j < 16; j++)
					t[j] ^= s[(j + p) & 15];
			Next();
		}

	for(int j = 0; j < 16; j++)
		s[(j + p) & 15] = t[j];
}
    
}
