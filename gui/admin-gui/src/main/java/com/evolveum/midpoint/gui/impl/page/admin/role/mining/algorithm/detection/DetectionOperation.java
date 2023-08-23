/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.detection;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.object.DetectionOption;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils.Handler;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.MiningRoleTypeChunk;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.MiningUserTypeChunk;

import java.util.List;

public interface DetectionOperation {

    List<DetectedPattern> performUserBasedDetection(List<MiningRoleTypeChunk> miningRoleTypeChunks,
            DetectionOption roleAnalysisDetectionOptionType, Handler handler);

    List<DetectedPattern> performRoleBasedDetection(List<MiningUserTypeChunk> miningRoleTypeChunks,
            DetectionOption roleAnalysisDetectionOptionType, Handler handler);

}
