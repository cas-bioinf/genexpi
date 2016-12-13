void CopyProfile(global const T_Value* geneProfilesGlobal, const uint numGenes,const uint numTime, const uint geneIndex,
		local T_Value* localProfiles, const uint localStride, const uint localOffset)
{
    if(get_local_id(1) == 0)
    {	
		for(int i = 0; i < numTime; i++)
		{
			localProfiles[(i * localStride) + localOffset] = geneProfilesGlobal[i * numGenes + geneIndex];
		}
    }
}
