package titanicsend.pattern.yoffa;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.will.shaders.*;
import titanicsend.pattern.yoffa.client.reponse.Shader;
import titanicsend.pattern.yoffa.effect.PulseEffect;
import titanicsend.pattern.yoffa.effect.ShimmeringEffect;
import titanicsend.pattern.yoffa.effect.AlternatingDotsEffect;
import titanicsend.pattern.yoffa.effect.BreathingDotsEffect;
import titanicsend.pattern.yoffa.effect.shaders.*;
import titanicsend.pattern.yoffa.framework.ConstructedPattern;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.effect.shaders.OutrunGridShader;
import titanicsend.pattern.yoffa.effect.shaders.NeonHeartShader;
import titanicsend.pattern.yoffa.media.BasicVideoPatternEffect;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;

import java.util.List;

@SuppressWarnings("unused")
public class PatternConfig {

    public static Class[] getPatterns() {
        return PatternConfig.class.getDeclaredClasses();
    }

    @LXCategory("Yoffa Panel Combo")
    public static class StarryHeart extends ConstructedPattern {
        public StarryHeart(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(
                    new NeonHeartShader(PatternTarget.splitPanelSections(this)),
                    new BreathingDotsEffect(PatternTarget.splitPanelSections(this))
                            .setShouldBlend(true)
            );
        }
    }

    @LXCategory("Yoffa Panel Combo")
    public static class StarryOutrun extends ConstructedPattern {
        public StarryOutrun(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(
                    new OutrunGridShader(PatternTarget.splitPanelSections(this)),
                    new AlternatingDotsEffect(PatternTarget.splitPanelSections(this))
                            .setHorizon(OutrunGridShader.HORIZON_Y)
                            .setShouldBlend(true)
            );
        }
    }

    @LXCategory("Yoffa Panel Shader")
    public static class RainbowSwirlPanels extends ConstructedPattern {
        public RainbowSwirlPanels(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new RainbowSwirlShader(PatternTarget.splitPanelSections(this)));
        }
    }

    @LXCategory("Yoffa Edge Shader")
    public static class RainbowSwirlEdges extends ConstructedPattern {
        public RainbowSwirlEdges(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new RainbowSwirlShader(PatternTarget.allEdgesAsCanvas(this)));
        }
    }

    @LXCategory("Yoffa Panel Shader")
    public static class NeonBarsPanels extends ConstructedPattern {
        public NeonBarsPanels(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NeonBarsShader(PatternTarget.splitPanelSections(this)));
        }
    }

    @LXCategory("Yoffa Edge Shader")
    public static class NeonBarsEdges extends ConstructedPattern {
        public NeonBarsEdges(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NeonBarsShader(PatternTarget.allEdgesAsCanvas(this)));
        }
    }

    @LXCategory("Yoffa Panel Shader")
    public static class NeonCellsPanels extends ConstructedPattern {
        public NeonCellsPanels(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NeonCellsShader(PatternTarget.splitPanelSections(this)));
        }
    }

    @LXCategory("Yoffa Panel Shader")
    public static class BasicElectricPanels extends ConstructedPattern {
        public BasicElectricPanels(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new ElectricShader(PatternTarget.splitPanelSections(this)));
        }
    }

    @LXCategory("Yoffa Edge Shader")
    public static class BasicElectricEdges extends ConstructedPattern {
        public BasicElectricEdges(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new ElectricShader(PatternTarget.allEdgesAsCanvas(this)));
        }
    }

    @LXCategory("Yoffa Panel Shader")
    public static class NeonHeart extends ConstructedPattern {
        public NeonHeart(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NeonHeartShader(PatternTarget.splitPanelSections(this)));
        }
    }

    @LXCategory("Yoffa Panel Shader")
    public static class Outrun extends ConstructedPattern {
        public Outrun(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new OutrunGridShader(PatternTarget.splitPanelSections(this)));
        }
    }

    @LXCategory("Yoffa Panel Shader")
    public static class WaterPanels extends ConstructedPattern {
        public WaterPanels(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new WaterShader(PatternTarget.splitPanelSections(this)));
        }
    }

    @LXCategory("Yoffa Edge Shader")
    public static class WaterEdges extends ConstructedPattern {
        public WaterEdges(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new WaterShader(PatternTarget.allEdgesAsCanvas(this)));
        }
    }

    @LXCategory("Yoffa Panel Shader")
    public static class WavyPanels extends ConstructedPattern {
        public WavyPanels(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new WavyShader(PatternTarget.splitPanelSections(this)));
        }
    }

    @LXCategory("Yoffa Panel Shader")
    public static class NeonSnake extends ConstructedPattern {
        public NeonSnake(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NeonSnakeShader(PatternTarget.allPanelsAsCanvas(this)));
        }
    }

    @LXCategory("Yoffa Edge Shader")
    public static class WavyEdges extends ConstructedPattern {
        public WavyEdges(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new WavyShader(PatternTarget.allEdgesAsCanvas(this)));
        }
    }

    @LXCategory("Yoffa Panel Organic")
    public static class PulseCenter extends ConstructedPattern {
        public PulseCenter(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new PulseEffect(PatternTarget.allPanelsAsCanvas(this)));
        }
    }

    @LXCategory("Yoffa Panel Organic")
    public static class PulseSide extends ConstructedPattern {
        public PulseSide(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new PulseEffect(PatternTarget.allPanelsAsCanvas(this)).setOrigin(0, 0, 0));
        }
    }

    @LXCategory("Yoffa Panel Organic")
    public static class AlternatingDots extends ConstructedPattern {
        public AlternatingDots(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new AlternatingDotsEffect(PatternTarget.allPanelsAsCanvas(this)));
        }
    }

    @LXCategory("Yoffa Panel Organic")
    public static class BreathingDots extends ConstructedPattern {
        public BreathingDots(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new BreathingDotsEffect(PatternTarget.allPanelsAsCanvas(this)));
        }
    }

    @LXCategory("Yoffa Edge Organic")
    public static class PowerGrid extends ConstructedPattern {
        public PowerGrid(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new ShimmeringEffect(PatternTarget.allEdgesAsCanvas(this)));
        }
    }

    @LXCategory("Video Patterns")
    public static class BasicVideoPattern extends ConstructedPattern {
        public BasicVideoPattern(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new BasicVideoPatternEffect(PatternTarget.splitPanelSections(this)));
        }
    }

    //below patterns for on the fly testing
    @LXCategory("Video Patterns")
    public static class FullscreenVideoA extends ConstructedPattern {
        public FullscreenVideoA(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new BasicVideoPatternEffect(PatternTarget.allPanelsAsCanvas(this),
                    "resources/pattern/test_vid_a.mp4"));
        }
    }

    @LXCategory("Video Patterns")
    public static class FullscreenVideoB extends ConstructedPattern {
        public FullscreenVideoB(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new BasicVideoPatternEffect(PatternTarget.allPanelsAsCanvas(this),
                    "resources/pattern/test_vid_b.mp4"));
        }
    }

    @LXCategory("DREVO Shaders")
    public static class RhythmicFlashStatic extends ConstructedPattern {
        public RhythmicFlashStatic(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new RhythmicFlashingStatic(PatternTarget.allPanelsAsCanvas(this)));
        }
    }

    @LXCategory("DREVO Shaders")
    public static class MatrixScroller extends ConstructedPattern {
        public MatrixScroller(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {

            return List.of(new MatrixScrolling(PatternTarget.allPanelsAsCanvas(this)));
        }
    }

    @LXCategory("Native Shaders")
    public static class ShaderToyPattern extends ConstructedPattern {
        public ShaderToyPattern(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect(PatternTarget.splitPanelSections(this)));
        }
    }

}
