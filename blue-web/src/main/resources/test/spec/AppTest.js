describe('Unit: Start application', function() {

  it("The app must start", function() {
    expect(angular.module("bluelatex")).toBeDefined();
    expect(angular.module('bluelatex.Paper.Controllers.EditPaper')).toBeDefined();
    expect(angular.module('bluelatex.Paper.Controllers.InitPaper')).toBeDefined();
    expect(angular.module('bluelatex.Paper.Controllers.LatexPaper')).toBeDefined();
    expect(angular.module('bluelatex.Paper.Controllers.NewPaper')).toBeDefined();
    expect(angular.module('bluelatex.Paper.Controllers.Papers')).toBeDefined();
    expect(angular.module('bluelatex.Latex.Directives.Preview')).toBeDefined();
    expect(angular.module('bluelatex.Paper.Directives.Toc')).toBeDefined();
    expect(angular.module('bluelatex.Paper')).toBeDefined();
    expect(angular.module('bluelatex.Paper.Services.Ace')).toBeDefined();
    expect(angular.module('bluelatex.Paper.Services.Latex')).toBeDefined();
    expect(angular.module('bluelatex.Paper.Services.Paper')).toBeDefined();
    expect(angular.module("bluelatex.Latex.Services.SyncTexParser")).toBeDefined();
    
  });
})