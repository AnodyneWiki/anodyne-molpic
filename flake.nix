{
  description = "Minimal example of building Kotlin with Gradle and Nix";

  inputs = { 
    nixpkgs.url = "github:nixos/nixpkgs?ref=nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
    flake-utils.inputs.systems.follows = "systems";
    build-gradle-application.url = "github:raphiz/buildGradleApplication";
  };
  outputs = { self, systems, nixpkgs, build-gradle-application, flake-utils, ... }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; overlays = [ build-gradle-application.overlays.default ]; };
        molpic = pkgs.buildGradleApplication {
          pname = "anodyne-molpic";
          version = "0.1.0";
          src = ./.;
        };
      in {
        devShells.default = pkgs.mkShell {
          packages = [
            molpic
          ];
        };
        packages.default = molpic;
      });
}
