{ pkgs ? import <nixpkgs> { }, fixDarwinDylibNames, stdenv, gnumake, gcc, lib }:

pkgs.stdenv.mkDerivation rec {
  pname = "libpg_query";
  version = "15-4.2.3";

  src = pkgs.fetchFromGitHub {
    owner = "pganalyze";
    repo = "libpg_query";
    rev = version;
    sha256 = "sha256-/HUg6x0il5WxENmgR3slu7nmXTKv6YscjpX569Dztko=";
  };

  nativeBuildInputs = [ gnumake gcc ]
    ++ (lib.optionals pkgs.stdenv.hostPlatform.isDarwin [ fixDarwinDylibNames ]);

  buildPhase = ''
    make build_shared
  '';

  installPhase = ''
    mkdir -p $out/lib
    cp libpg_query.* $out/lib/
  '';

  meta = with lib; {
    description = "PostgreSQL parser library";
    homepage = "https://github.com/pganalyze/libpg_query";
    license = licenses.mit;
    maintainers = with maintainers; [ pkgs.thenonameguy ];
  };
}
