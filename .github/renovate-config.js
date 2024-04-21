module.exports = {
    branchPrefix: 'renovate/',
    username: 'Renovate Bot',
    gitAuthor: 'Renovate Bot <bot@renovateapp.com>',
    platform: 'github',
    repositories: ['jbwheatley/pact4s'],
    "baseBranches": ["main"],
    "packageRules": [{
		"packagePatterns": ["*"],
		"enabled": false
	}, {
		"paths": [".github/workflows/*"],
		"enabled": true
	}],
};
