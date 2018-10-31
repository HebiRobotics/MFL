% http://stackoverflow.com/questions/36025747/matlab-documentation-on-handle-variables-and-mat-files

%% create and save the objects
% Create 2 object variables. 2nd
% one has a property that refers to
% same object as the first one
objA = HandleClass;
objA.myPropA = 5;
objB = OtherClass;
objB.myObjA = objA;
objC = HandleClass;
objC.myPropA = objA;

save('../mcos/handles.mat', 'objA', 'objB', 'objC');

%% clear and load them back
clear
load('../mcos/handles.mat')
