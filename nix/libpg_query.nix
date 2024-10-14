{ pkgs ? import <nixpkgs> { }, fixDarwinDylibNames, stdenv, gnumake, gcc, lib }:

pkgs.stdenv.mkDerivation rec {
  pname = "libpg_query";
  version = "16-5.1.0";

  src = pkgs.fetchFromGitHub {
    owner = "pganalyze";
    repo = "libpg_query";
    rev = version;
    sha256 = "sha256-X48wjKdgkAc4wUubQ5ip1zZYiCKzQJyQTgGvO/pOY3I=";
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
