package emanondev.deepdungeons.reward;

import emanondev.deepdungeons.generic.AProviderManager;

public class RewardManager extends AProviderManager<RewardProvider> {

	public static final String NAME = "Reward";

	public RewardManager() {
		try {
			this.register(new DracarysGuiRewardProvider());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
