package org.alice.serialization.tweedle;

import org.lgna.project.code.CodeOrganizer;

import java.util.*;

/**
 * Static data tables used by {@link TweedleEncoder} for type/member renaming,
 * parameter labeling, and code organization. Extracted to keep the encoder
 * focused on traversal logic.
 */
final class TweedleEncoderData {

  private TweedleEncoderData() {
  }

  static final Map<String, CodeOrganizer.CodeOrganizerDefinition> codeOrganizerDefinitionMap = new HashMap<>();
  static final Set<String> angleMembers = new HashSet<>();
  static final Map<String, String> typesToRename = new HashMap<>();
  static final Map<String, String> typesWithAddedCode = new HashMap<>();
  static final Map<String, String> membersToRename = new HashMap<>();
  static final Map<String, String[]> methodsMissingParameterNames = new HashMap<>();
  static final Map<String, Map<String, String>> methodsWithWrappedArgs = new HashMap<>();
  static final Map<String, String> optionalParamsToWrap = new HashMap<>();
  static final Map<String, String> methodParamsToRelabel = new HashMap<>();
  static final Map<String, Map<String, String>> constructorsWithRelabeledParams = new HashMap<>();
  static final Set<String> systemIdentifiers = new HashSet<>();

  static {
    codeOrganizerDefinitionMap.put("Scene", CodeOrganizer.sceneClassCodeOrganizer);
    codeOrganizerDefinitionMap.put("Program", CodeOrganizer.programClassCodeOrganizer);

    membersToRename.put("rint", "round");
    membersToRename.put("ceil", "ceiling");
    typesToRename.put("IntegerUtilities", "$WholeNumber");
    membersToRename.put("toFlooredInteger", "floor");
    membersToRename.put("toRoundedInteger", "round");
    membersToRename.put("toCeilingedInteger", "ceiling");
    typesToRename.put("RandomUtilities", "$Random");
    membersToRename.put("nextIntegerFrom0ToNExclusive", "wholeNumberFrom0ToNExclusive");
    membersToRename.put("nextIntegerFromAToBExclusive", "wholeNumberFromAToBExclusive");
    membersToRename.put("nextIntegerFromAToBInclusive", "wholeNumberFromAToBInclusive");
    membersToRename.put("nextDoubleInRange", "decimalNumberInRange");
    membersToRename.put("nextBoolean", "boolean");
    membersToRename.put("COMBINE", "OVERLAP");

    typesToRename.put("Double", "DecimalNumber");
    typesToRename.put("Double[]", "DecimalNumber[]");
    typesToRename.put("Integer", "WholeNumber");
    typesToRename.put("Integer[]", "WholeNumber[]");
    typesToRename.put("String", "TextString");
    typesToRename.put("String[]", "TextString[]");
    typesToRename.put("SandDunes", "Terrain");
    typesToRename.put("Visual", "SThing");
    typesToRename.put("Visual[]", "SThing[]");
    typesToRename.put("StartOcclusionEvent", "ModelInteractionEvent");
    typesToRename.put("EndOcclusionEvent", "ModelInteractionEvent");
    typesToRename.put("StartCollisionEvent", "ThingInteractionEvent");
    typesToRename.put("EndCollisionEvent", "ThingInteractionEvent");
    typesToRename.put("EnterProximityEvent", "ThingInteractionEvent");
    typesToRename.put("ExitProximityEvent", "ThingInteractionEvent");
    typesToRename.put("EnterViewEvent", "ViewEvent");
    typesToRename.put("ExitViewEvent", "ViewEvent");
    typesToRename.put("MultipleEventPolicy", "OverlappingEventPolicy");
    typesToRename.put("ElderPersonResource", "PersonResource");
    typesToRename.put("AdultPersonResource", "PersonResource");
    typesToRename.put("TeenPersonResource", "PersonResource");
    typesToRename.put("ChildPersonResource", "PersonResource");
    typesToRename.put("ToddlerPersonResource", "PersonResource");
    typesToRename.put("BipedPose", "JointedModelPose");
    typesToRename.put("FlyerPose", "JointedModelPose");
    typesToRename.put("SlithererPose", "JointedModelPose");
    typesToRename.put("SwimmerPose", "JointedModelPose");
    typesToRename.put("QuadrupedPose", "JointedModelPose");

    typesWithAddedCode.put(
        "Person",
        """
              SJoint getRightEye() {
                SJoint eye <- super.getRightEye();
                $SceneGraph.trackFacialJoint(joint: eye, person: this);
                return eye;
              }
              SJoint getLeftEye() {
                SJoint eye <- super.getLeftEye();
                $SceneGraph.trackFacialJoint(joint: eye, person: this);
                return eye;
              }
              SJoint getLeftEyelid() {
                SJoint eyelid <- super.getLeftEyelid();
                $SceneGraph.trackFacialJoint(joint: eyelid, person: this);
                return eyelid;
              }
              SJoint getRightEyelid() {
                SJoint eyelid <- super.getRightEyelid();
                $SceneGraph.trackFacialJoint(joint: eyelid, person: this);
                return eyelid;
              }
              SJoint getMouth() {
                SJoint mouth <- super.getMouth();
                $SceneGraph.trackFacialJoint(joint: mouth, person: this);
                return mouth;
              }
            """);

    methodsMissingParameterNames.put("say", new String[] {"text"});
    methodsMissingParameterNames.put("think", new String[] {"text"});
    methodsMissingParameterNames.put("setJointedModelResource", new String[] {"resource"});
    methodsMissingParameterNames.put("setOrientationRelativeToVehicle", new String[] {"orientation"});
    methodsMissingParameterNames.put("setPositionRelativeToVehicle", new String[] {"position"});
    methodsMissingParameterNames.put("setVehicle", new String[] {"vehicle"});
    methodsMissingParameterNames.put("setFloorPaint", new String[] {"paint"});
    methodsMissingParameterNames.put("setWallPaint", new String[] {"paint"});
    methodsMissingParameterNames.put("setCeilingPaint", new String[] {"paint"});
    methodsMissingParameterNames.put("setOpacity", new String[] {"opacity"});
    methodsMissingParameterNames.put("strikePose", new String[] {"pose"});
    methodsMissingParameterNames.put("pow", new String[] {"b", "power"});
    methodsMissingParameterNames.put("nextIntegerFromAToBExclusive", new String[] {"a", "b"});
    methodsMissingParameterNames.put("nextIntegerFromAToBInclusive", new String[] {"a", "b"});
    methodsMissingParameterNames.put("nextIntegerFrom0ToNExclusive", new String[] {"n"});
    methodsMissingParameterNames.put("toFlooredInteger", new String[] {"decimalNumber"});
    methodsMissingParameterNames.put("toRoundedInteger", new String[] {"decimalNumber"});
    methodsMissingParameterNames.put("toCeilingedInteger", new String[] {"decimalNumber"});
    methodsMissingParameterNames.put("abs", new String[] {"number"});
    // Min and max cover both decimal and whole
    methodsMissingParameterNames.put("min", new String[] {"a", "b"});
    methodsMissingParameterNames.put("max", new String[] {"a", "b"});

    methodsMissingParameterNames.put("floor", new String[] {"decimalNumber"});
    methodsMissingParameterNames.put("rint", new String[] {"decimalNumber"});
    methodsMissingParameterNames.put("ceil", new String[] {"decimalNumber"});
    methodsMissingParameterNames.put("nextDoubleInRange", new String[] {"a", "b"});
    methodsMissingParameterNames.put("sqrt", new String[] {"decimalNumber"});
    angleMembers.add("sin");
    angleMembers.add("cos");
    angleMembers.add("tan");
    angleMembers.add("asin");
    angleMembers.add("acos");
    angleMembers.add("atan");
    angleMembers.add("atan2");
    angleMembers.add("PI");
    methodsMissingParameterNames.put("sin", new String[] {"radians"});
    methodsMissingParameterNames.put("cos", new String[] {"radians"});
    methodsMissingParameterNames.put("tan", new String[] {"radians"});
    methodsMissingParameterNames.put("asin", new String[] {"sin"});
    methodsMissingParameterNames.put("acos", new String[] {"cos"});
    methodsMissingParameterNames.put("atan", new String[] {"tan"});
    methodsMissingParameterNames.put("atan2", new String[] {"y", "x"});
    methodsMissingParameterNames.put("exp", new String[] {"power"});
    methodsMissingParameterNames.put("log", new String[] {"x"});
    methodsMissingParameterNames.put("contentEquals", new String[] {"text"});
    methodsMissingParameterNames.put("equalsIgnoreCase", new String[] {"text"});
    methodsMissingParameterNames.put("startsWith", new String[] {"text"});
    methodsMissingParameterNames.put("endsWith", new String[] {"text"});
    methodsMissingParameterNames.put("contains", new String[] {"text"});
    methodsMissingParameterNames.put("getJoint", new String[] {"name"});

    Map<String, String> duration = new HashMap<>();
    duration.put("duration", "new Duration(seconds: ");
    methodsWithWrappedArgs.put("delay", duration);
    Map<String, String> frequency = new HashMap<>();
    frequency.put("frequency", "new Duration(seconds: ");
    methodsWithWrappedArgs.put("addTimeListener", frequency);
    Map<String, String> amount = new HashMap<>();
    amount.put("amount", "new Angle(revolutions: ");
    methodsWithWrappedArgs.put("roll", amount);
    methodsWithWrappedArgs.put("turn", amount);
    Map<String, String> angle = new HashMap<>();
    angle.put("angle", "new Angle(revolutions: ");
    methodsWithWrappedArgs.put("setHorizontalViewingAngle", angle);
    methodsWithWrappedArgs.put("setVerticalViewingAngle", angle);
    Map<String, String> density = new HashMap<>();
    density.put("density", "new Portion(portion: ");
    methodsWithWrappedArgs.put("setFogDensity", density);
    Map<String, String> opacity = new HashMap<>();
    opacity.put("opacity", "new Portion(portion: ");
    methodsWithWrappedArgs.put("setOpacity", opacity);
    Map<String, String> userJointName = new HashMap<>();
    userJointName.put("name", "(\"u_\" .. ");
    methodsWithWrappedArgs.put("getJoint", userJointName);

    optionalParamsToWrap.put("duration", "new Duration(seconds: ");

    methodParamsToRelabel.put("multipleEventPolicy", "overlappingEventPolicy");

    Map<String, String> sizeParams = new HashMap<>();
    sizeParams.put("leftToRight", "width");
    sizeParams.put("bottomToTop", "height");
    sizeParams.put("frontToBack", "depth");
    constructorsWithRelabeledParams.put("Size", sizeParams);
    Map<String, String> positionParams = new HashMap<>();
    positionParams.put("right", "x");
    positionParams.put("up", "y");
    positionParams.put("backward", "z");
    constructorsWithRelabeledParams.put("Position", positionParams);
    Map<String, String> imageParams = new HashMap<>();
    imageParams.put("imageResource", "resource");
    constructorsWithRelabeledParams.put("ImageSource", imageParams);

    systemIdentifiers.add("args");
    systemIdentifiers.add("index");
    systemIdentifiers.add("value");
    systemIdentifiers.add("event");
    systemIdentifiers.add("resource");
    systemIdentifiers.add("isActive");
    systemIdentifiers.add("activationCount");
    systemIdentifiers.add("myScene");
    systemIdentifiers.add("story");
    systemIdentifiers.add("ground");
    systemIdentifiers.add("camera");
    systemIdentifiers.add("vrUser");
  }
}
