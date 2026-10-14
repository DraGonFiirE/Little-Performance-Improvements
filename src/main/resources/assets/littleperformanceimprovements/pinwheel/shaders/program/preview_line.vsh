in vec3 Position;
in vec4 Color;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
out vec4 vertexColor;
out vec3 vertexViewPosition;
out vec3 vertexLocalPosition;
void main(){vertexLocalPosition=Position;vec4 view=ModelViewMat*vec4(Position,1);vertexViewPosition=view.xyz;gl_Position=ProjMat*view;vertexColor=Color;}
