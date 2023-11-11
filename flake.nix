{
  description = "pg-query-clj - Clojure library to parse, deparse and normalize SQL queries using the PostgreSQL query parser";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
    clj-nix.url = "github:jlesquembre/clj-nix";
  };

  outputs = { self, nixpkgs, flake-utils, clj-nix }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
        cljpkgs = clj-nix.packages.${system};
        libpg-query-15 = pkgs.callPackage ./nix/libpg_query.nix { };
      in
      {

        packages = {
          inherit libpg-query-15;
          default = cljpkgs.mkCljLib {
            projectSrc = ./.;
            name = "io.schemamap/pg-query-clj";
            version = "DEV";
            buildCommand = "clj -T:build ci";
          };
        };
        devShells.default = pkgs.mkShell {
          buildInputs = [
            # provides deps-lock library for updating deps-lock.json from deps.edn
            clj-nix.packages.${system}.deps-lock
            pkgs.clojure
          ];
          shellHook =
            let
              # Simulating JNA Platform#getNativeLibraryResourcePrefix()
              # Flip arch-os from Nix to os-arch
               parts = pkgs.lib.splitString "-" pkgs.system;
               os-arch-prefix = builtins.concatStringsSep "-" (pkgs.lib.reverseList parts);
              lib-folder = "./lib/${os-arch-prefix}";
            in
            ''
              # symlink the platform specific shared library to the lib folder
              mkdir -p ${lib-folder}
              ln -sf ${libpg-query-15}/lib/* ${lib-folder}
            '';
        };
      });
}
