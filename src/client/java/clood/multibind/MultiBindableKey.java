package clood.multibind;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.Collection;

public interface MultiBindableKey {
	InputConstants.Key multibind$getPrimaryKey();

	Collection<InputConstants.Key> multibind$getAdditionalKeys();

	void multibind$replaceKey(InputConstants.Key key);

	void multibind$toggleAdditionalKey(InputConstants.Key key);

	void multibind$clearAdditionalKeys();
}